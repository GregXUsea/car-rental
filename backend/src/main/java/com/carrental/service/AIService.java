package com.carrental.service;

import com.carrental.dto.AIRecommendResult;
import com.carrental.entity.Car;
import com.carrental.entity.MaintenanceRecord;
import com.carrental.mapper.MaintenanceRecordMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class AIService {

    @Value("${spark.api-key}")
    private String apiKey;

    @Value("${spark.api-secret}")
    private String apiSecret;

    @Value("${spark.base-url}")
    private String baseUrl;

    @Value("${spark.model}")
    private String model;

    @Autowired
    private CarService carService;

    @Autowired
    private MaintenanceRecordMapper maintenanceRecordMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();

    public AIRecommendResult recommendCars(String userRequirement) {
        List<Car> availableCars = carService.listAvailable();

        // 先用本地关键词匹配
        AIRecommendResult localResult = fallbackRecommend(userRequirement, availableCars);

        // 如果 API key 未配置，使用本地关键词匹配兜底
        if (apiKey == null || apiKey.equals("your-spark-api-key-here") || apiKey.isBlank()) {
            return localResult;
        }

        StringBuilder carListStr = new StringBuilder();
        for (Car car : availableCars) {
            carListStr.append(String.format(
                    "ID:%d, %s %s, %s, %d座, %.0f元/天, %s, 里程%dkm, %s\n",
                    car.getId(), car.getBrand(), car.getModel(), car.getColor(),
                    car.getSeats(), car.getPricePerDay(), car.getCategory(),
                    car.getMileage(), car.getDescription()));
        }

        String systemPrompt = "你是一个专业的汽车租赁顾问AI助手。用户会描述他们的用车需求，你需要从可用车辆列表中推荐最匹配的车型。" +
                "请用中文回答，以JSON格式返回推荐结果，格式如下：\n" +
                "{\"summary\":\"总体推荐理由\",\"recommendations\":[{\"carId\":车辆ID,\"reason\":\"推荐理由\",\"matchScore\":\"匹配度如95%\"}]}\n" +
                "铁规：1)用户提到人数时，绝不推荐座位数不够的车 2)如果所有车都坐不下，建议租多辆车组合，不要硬推不够座位的车 3)宁缺毋滥，只推荐真正合适的车。按匹配度从高到低排序。只返回JSON，不要其他文字。";

        String userPrompt = String.format("我的需求：%s\n\n可用车辆列表（注意座位数和状态）：\n%s", userRequirement, carListStr);

        try {
            String response = callSpark(systemPrompt, userPrompt);
            AIRecommendResult aiResult = parseRecommendResult(response, availableCars);
            // 硬过滤：AI可能忽略座位数限制，再次过滤（用全量库存做组合计算）
            aiResult = filterBySeats(aiResult, userRequirement, availableCars);
            aiResult.setPoweredBy("AI");
            return aiResult;
        } catch (Exception e) {
            localResult.setPoweredBy("本地");
            return localResult;
        }
    }

    public Map<String, Object> getMaintenancePrediction(Long carId) {
        Car car = carService.getById(carId);
        if (car == null) {
            throw new RuntimeException("车辆不存在");
        }

        List<MaintenanceRecord> records = maintenanceRecordMapper.selectList(
                new LambdaQueryWrapper<MaintenanceRecord>()
                        .eq(MaintenanceRecord::getCarId, carId)
                        .orderByDesc(MaintenanceRecord::getMaintenanceDate));

        StringBuilder recordStr = new StringBuilder();
        for (MaintenanceRecord r : records) {
            recordStr.append(String.format("日期:%s, 里程:%dkm, 类型:%s, 描述:%s, 费用:%.0f元\n",
                    r.getMaintenanceDate(), r.getMileageAtMaintenance(),
                    r.getMaintenanceType(), r.getDescription(), r.getCost()));
        }

        String systemPrompt = "你是一个专业的汽车维护顾问。根据车辆信息和历史维护记录，预测下次维护时间和建议。" +
                "用中文回答，返回JSON格式：\n" +
                "{\"nextMaintenanceDate\":\"预计日期\",\"nextMaintenanceType\":\"保养类型\",\"suggestions\":[\"建议1\",\"建议2\"],\"riskLevel\":\"低/中/高\"}\n" +
                "只返回JSON。";

        String userPrompt = String.format(
                "车辆：%s %s, 当前里程:%dkm, 上次保养:%s\n\n历史维护记录：\n%s",
                car.getBrand(), car.getModel(), car.getMileage(),
                car.getLastMaintainDate(), recordStr);

        try {
            String response = callSpark(systemPrompt, userPrompt);
            return objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("nextMaintenanceDate", "数据不足，无法准确预测");
            fallback.put("nextMaintenanceType", "常规保养");
            fallback.put("suggestions", List.of("建议定期检查机油", "注意轮胎磨损情况"));
            fallback.put("riskLevel", "中");
            return fallback;
        }
    }

    private String callSpark(String systemPrompt, String userPrompt) throws IOException {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)));
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 3000);
        // 关闭深度思考，节省token
        requestBody.put("thinking", Map.of("type", "disabled"));

        String json = objectMapper.writeValueAsString(requestBody);

        // 支持两种鉴权：API Password 直接用，或 APIKey:APISecret 拼接
        String auth = (apiSecret == null || apiSecret.isBlank()) ? apiKey : (apiKey + ":" + apiSecret);

        Request request = new Request.Builder()
                .url(baseUrl + "/chat/completions")
                .addHeader("Authorization", "Bearer " + auth)
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .post(RequestBody.create(json, MediaType.parse("application/json; charset=utf-8")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errBody = response.body() != null ? response.body().string() : "";
                throw new IOException("星火API调用失败，状态码: " + response.code() + ", 响应: " + errBody);
            }
            // 显式按UTF-8读取响应
            byte[] respBytes = response.body().bytes();
            String body = new String(respBytes, StandardCharsets.UTF_8);
            JsonNode root = objectMapper.readTree(body);
            return root.path("choices").get(0).path("message").path("content").asText();
        }
    }

    private AIRecommendResult parseRecommendResult(String response, List<Car> availableCars) {
        try {
            // 清理可能的markdown包裹
            String json = response.trim();
            if (json.startsWith("```")) {
                json = json.replaceAll("```json?", "").replaceAll("```", "").trim();
            }

            JsonNode root = objectMapper.readTree(json);
            AIRecommendResult result = new AIRecommendResult();
            result.setSummary(root.path("summary").asText());

            Map<Long, Car> carMap = availableCars.stream()
                    .collect(Collectors.toMap(Car::getId, c -> c));

            List<AIRecommendResult.RecommendItem> items = new ArrayList<>();
            JsonNode recs = root.path("recommendations");
            for (JsonNode rec : recs) {
                AIRecommendResult.RecommendItem item = new AIRecommendResult.RecommendItem();
                Long carId = rec.path("carId").asLong();
                item.setCar(carMap.get(carId));
                item.setReason(rec.path("reason").asText());
                item.setMatchScore(rec.path("matchScore").asText());
                if (item.getCar() != null) {
                    items.add(item);
                }
            }
            result.setRecommendations(items);
            return result;
        } catch (Exception e) {
            return fallbackRecommend("", availableCars);
        }
    }

    /**
     * 硬过滤：AI可能忽略座位限制，将不满足座位数的推荐剔除。
     * 如果所有车都坐不下，用全量库存计算多车组合方案。
     */
    private AIRecommendResult filterBySeats(AIRecommendResult aiResult, String requirement, List<Car> allAvailableCars) {
        int requiredSeats = getRequiredSeats(requirement);
        if (requiredSeats <= 0) {
            return aiResult;
        }
        if (aiResult.getRecommendations() == null) {
            aiResult.setRecommendations(new ArrayList<>());
        }
        List<AIRecommendResult.RecommendItem> filtered = new ArrayList<>();
        for (AIRecommendResult.RecommendItem item : aiResult.getRecommendations()) {
            if (item.getCar() != null && item.getCar().getSeats() >= requiredSeats) {
                filtered.add(item);
            }
        }
        if (!filtered.isEmpty()) {
            aiResult.setRecommendations(filtered);
            return aiResult;
        }

        // 所有车都坐不下 → 用全量库存计算最优多车组合
        return buildMultiCarSuggestion(allAvailableCars, requiredSeats, requirement);
    }

    /**
     * 从需求中提取所需座位数，返回0表示未明确指定
     */
    private int getRequiredSeats(String req) {
        // 数字 + 人/个/口/座
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("(\\d+)\\s*[人个口座]");
        java.util.regex.Matcher m = p.matcher(req);
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }
        // 中文数字
        if (req.contains("七人") || req.contains("七口") || req.contains("七座")) return 7;
        if (req.contains("六人") || req.contains("六口") || req.contains("六座") || req.contains("大家庭")) return 6;
        if (req.contains("五人") || req.contains("五口") || req.contains("五座")) return 5;
        if (req.contains("四人") || req.contains("四口") || req.contains("四座")) return 4;
        if (req.contains("三人") || req.contains("三口") || req.contains("三座")) return 3;
        if (req.contains("两人") || req.contains("两口") || req.contains("两座")) return 2;
        if (req.contains("一人") || req.contains("单")) return 1;
        // "多人"按6人处理
        if (req.contains("多人") || req.contains("一家人") || req.contains("全家")) return 6;
        return 0;
    }

    private AIRecommendResult fallbackRecommend(String requirement, List<Car> cars) {
        AIRecommendResult result = new AIRecommendResult();

        String req = requirement.toLowerCase();
        int requiredSeats = getRequiredSeats(req);

        // 先按座位数硬过滤：需求人数超过座位数直接排除
        List<Car> filtered = new ArrayList<>();
        for (Car car : cars) {
            if (requiredSeats <= 0 || car.getSeats() >= requiredSeats) {
                filtered.add(car);
            }
        }

        // 如果过滤后没有车 → 建议租多辆
        if (filtered.isEmpty()) {
            return buildMultiCarSuggestion(cars, requiredSeats, req);
        }

        List<Car> scored = new ArrayList<>(filtered);

        // 根据关键词给车辆打分排序
        final int reqSeats = requiredSeats;
        scored.sort((a, b) -> {
            int scoreA = calcMatchScore(a, req, reqSeats);
            int scoreB = calcMatchScore(b, req, reqSeats);
            return scoreB - scoreA;
        });

        List<AIRecommendResult.RecommendItem> items = new ArrayList<>();
        int count = Math.min(3, scored.size());
        for (int i = 0; i < count; i++) {
            Car car = scored.get(i);
            AIRecommendResult.RecommendItem item = new AIRecommendResult.RecommendItem();
            item.setCar(car);
            item.setReason(buildReason(car, req));
            item.setMatchScore(calcMatchScore(car, req, reqSeats) + "%");
            items.add(item);
        }

        result.setSummary(String.format("为您从 %d 辆可用车辆中推荐了 %d 辆最匹配的车型", filtered.size(), items.size()));
        result.setRecommendations(items);
        return result;
    }

    /**
     * 人数超出所有车辆座位数时，计算最优多车组合方案（按总价最低优先）
     */
    private AIRecommendResult buildMultiCarSuggestion(List<Car> cars, int requiredSeats, String req) {
        AIRecommendResult result = new AIRecommendResult();

        // 方案1：同款车多辆 → 找总价最低的
        Car bestSingle = null;
        int bestSingleCount = 0;
        double bestSingleCost = Double.MAX_VALUE;

        for (Car car : cars) {
            int count = (int) Math.ceil((double) requiredSeats / car.getSeats());
            double totalCost = car.getPricePerDay().doubleValue() * count;
            if (totalCost < bestSingleCost) {
                bestSingleCost = totalCost;
                bestSingle = car;
                bestSingleCount = count;
            }
        }

        // 方案2：混合组合 → 大车+小车搭配，看能否更省
        Car cheap = cars.stream().min((a, b) -> Double.compare(
                a.getPricePerDay().doubleValue(), b.getPricePerDay().doubleValue())).orElse(bestSingle);
        Car spacious = cars.stream().max((a, b) -> Integer.compare(
                a.getSeats(), b.getSeats())).orElse(bestSingle);

        // 组合：1辆大车 + N辆便宜小车
        int remaining = requiredSeats - spacious.getSeats();
        int smallCount = remaining > 0 ? (int) Math.ceil((double) remaining / cheap.getSeats()) : 0;
        double mixedCost = spacious.getPricePerDay().doubleValue() + cheap.getPricePerDay().doubleValue() * smallCount;
        int mixedSeats = spacious.getSeats() + cheap.getSeats() * smallCount;

        // 选最优方案
        StringBuilder summary = new StringBuilder();
        List<AIRecommendResult.RecommendItem> items = new ArrayList<>();

        if (mixedCost < bestSingleCost && smallCount > 0 && smallCount < bestSingleCount) {
            // 混合方案更优
            summary.append(String.format("当前无%d座车，推荐混合方案：1辆%s%s（%d座）+ %d辆%s%s（%d座），共%d座，总价约¥%.0f/天。",
                    requiredSeats,
                    spacious.getBrand(), spacious.getModel(), spacious.getSeats(),
                    smallCount, cheap.getBrand(), cheap.getModel(), cheap.getSeats(),
                    mixedSeats, mixedCost));

            AIRecommendResult.RecommendItem item1 = new AIRecommendResult.RecommendItem();
            item1.setCar(spacious);
            item1.setReason(String.format("主力车型%d座，搭配小车降低成本", spacious.getSeats()));
            item1.setMatchScore("推荐组合");
            items.add(item1);

            AIRecommendResult.RecommendItem item2 = new AIRecommendResult.RecommendItem();
            item2.setCar(cheap);
            item2.setReason(String.format("经济补充，%d辆满足剩余%d人", smallCount, remaining));
            item2.setMatchScore("经济搭配");
            items.add(item2);
        } else {
            // 同款多辆更优
            summary.append(String.format("当前无%d座车，推荐%d辆%s%s（%d座×%d=%d座），总价约¥%.0f/天，是满足需求的最经济方案。",
                    requiredSeats, bestSingleCount,
                    bestSingle.getBrand(), bestSingle.getModel(), bestSingle.getSeats(), bestSingleCount,
                    bestSingleCount * bestSingle.getSeats(), bestSingleCost));

            AIRecommendResult.RecommendItem item = new AIRecommendResult.RecommendItem();
            item.setCar(bestSingle);
            item.setReason(String.format("最经济方案：%d辆满足%d人，总价¥%.0f/天", bestSingleCount, requiredSeats, bestSingleCost));
            item.setMatchScore("最经济组合");
            items.add(item);
        }

        result.setSummary(summary.toString());
        result.setRecommendations(items);
        return result;
    }

    private int calcMatchScore(Car car, String req, int requiredSeats) {
        int score = 50; // 基础分

        // 座位数匹配（改为核心权重）
        if (requiredSeats > 0) {
            if (car.getSeats() >= requiredSeats + 1) {
                score += 35; // 超出需求，空间更大
            } else if (car.getSeats() >= requiredSeats) {
                score += 30; // 刚好满足
            }
            // 不满足的已被过滤，不会到这里
        }

        // 价格匹配
        if (req.contains("便宜") || req.contains("经济") || req.contains("省钱") || req.contains("低端")) {
            if (car.getPricePerDay().doubleValue() < 200) score += 30;
            else if (car.getPricePerDay().doubleValue() < 300) score += 15;
        }
        if (req.contains("高档") || req.contains("豪华") || req.contains("高端") || req.contains("商务")) {
            if (car.getPricePerDay().doubleValue() >= 350) score += 30;
            else if (car.getPricePerDay().doubleValue() >= 250) score += 15;
        }
        if (req.contains("顶级") || req.contains("旗舰")) {
            if (car.getPricePerDay().doubleValue() >= 450) score += 35;
        }

        // 类型匹配
        if ((req.contains("suv") || req.contains("越野")) && "SUV".equals(car.getCategory())) score += 20;
        if ((req.contains("商务") || req.contains("接待")) && "MPV".equals(car.getCategory())) score += 20;
        if ((req.contains("电车") || req.contains("纯电") || req.contains("新能源")) && "新能源".equals(car.getCategory())) score += 20;
        if ((req.contains("轿车") || req.contains("通勤")) && "轿车".equals(car.getCategory())) score += 15;

        // 品牌关键词匹配
        String brandLower = car.getBrand().toLowerCase();
        String modelLower = car.getModel().toLowerCase();
        if (req.contains(brandLower) || req.contains(modelLower)) score += 20;

        return Math.min(98, score);
    }

    private String buildReason(Car car, String req) {
        StringBuilder reason = new StringBuilder();
        if (car.getPricePerDay().doubleValue() < 200) reason.append("经济实惠，");
        else if (car.getPricePerDay().doubleValue() < 350) reason.append("性价比出色，");
        else reason.append("豪华品质，");

        if (car.getSeats() >= 7) reason.append("7座大空间，");
        else if (car.getSeats() >= 6) reason.append("6座宽敞，");

        reason.append(car.getDescription());
        return reason.toString();
    }
}
