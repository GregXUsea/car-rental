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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class AIService {

    private static final Logger log = LoggerFactory.getLogger(AIService.class);

    @Value("${spark.api-password}")
    private String apiPassword;

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

    /** 多轮对话上下文存储：conversationId → 历史消息列表 */
    private final Map<String, List<Map<String, String>>> conversationStore = new ConcurrentHashMap<>();
    private static final int MAX_HISTORY_TURNS = 3; // 最多保留3轮对话

    // ==================== 公开方法 ====================

    /**
     * AI智能推荐车辆，支持多轮对话
     *
     * @param userRequirement 用户自然语言需求
     * @param conversationId  对话上下文ID（首次请求为null）
     * @return 推荐结果（含新的conversationId供追问使用）
     */
    public AIRecommendResult recommendCars(String userRequirement, String conversationId, boolean refresh) {
        final String originalReq = userRequirement != null ? userRequirement.trim() : "";
        long startTime = System.currentTimeMillis();
        log.info("AI推荐请求: requirement=\"{}\", conversationId={}, refresh={}", originalReq, conversationId, refresh);

        List<Car> availableCars = carService.listAvailable();
        log.info("可用车辆数: {}", availableCars.size());

        if (availableCars.isEmpty()) {
            AIRecommendResult empty = new AIRecommendResult();
            empty.setSummary("当前暂无可用车辆，请稍后再试");
            empty.setPoweredBy("系统");
            empty.setRecommendations(Collections.emptyList());
            return empty;
        }

        // 构建对话历史上下文
        String historyContext = buildHistoryContext(conversationId);

        // 如果 API 密码未配置，降级为本地推荐
        if (apiPassword == null || apiPassword.isBlank()) {
            log.warn("星火API密码未配置，使用本地推荐");
            AIRecommendResult localResult = fallbackRecommend(originalReq, availableCars, historyContext);
            localResult.setPoweredBy("本地");
            return localResult;
        }

        // 构建车辆列表文本（精简：仅保留推荐所需核心字段）
        StringBuilder carListStr = new StringBuilder();
        for (Car car : availableCars) {
            carListStr.append(String.format(
                    "ID:%d, %s %s, %d座, %.0f元/天, %s\n",
                    car.getId(), car.getBrand(), car.getModel(),
                    car.getSeats(), car.getPricePerDay(), car.getCategory()));
        }

        // 构建系统提示词（核心：让星火模型做需求理解+匹配+排序）
        String systemPrompt = buildSystemPrompt(historyContext, refresh);

        // 构建用户提示词（需求 + 车辆列表 + 可选组合）
        String userPrompt = buildUserPrompt(originalReq, availableCars, carListStr.toString());

        try {
            log.info("调用星火API, model={}, prompt总长度={}", model, userPrompt.length());
            String response = callSparkWithRetry(systemPrompt, userPrompt);
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("星火API响应耗时: {}ms", elapsed);

            AIRecommendResult aiResult = parseRecommendResult(response, availableCars, originalReq);
            aiResult.setPoweredBy("AI");

            // 保存对话上下文
            String newConvId = saveConversation(conversationId, originalReq, aiResult.getSummary());
            aiResult.setConversationId(newConvId);

            return aiResult;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("星火API调用失败(耗时{}ms)，降级为本地推荐: {}", elapsed, e.getMessage());
            AIRecommendResult localResult = fallbackRecommend(originalReq, availableCars, historyContext);
            localResult.setPoweredBy("本地");
            return localResult;
        }
    }

    /**
     * 车辆维护预测（AI-2选做功能）
     */
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
            String response = callSparkWithRetry(systemPrompt, userPrompt);
            return objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("维护预测AI调用失败: {}", e.getMessage());
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("nextMaintenanceDate", "数据不足，无法准确预测");
            fallback.put("nextMaintenanceType", "常规保养");
            fallback.put("suggestions", List.of("建议定期检查机油", "注意轮胎磨损情况"));
            fallback.put("riskLevel", "中");
            return fallback;
        }
    }

    // ==================== AI调用核心 ====================

    /**
     * 构建系统提示词
     */
    private String buildSystemPrompt(String historyContext, boolean refresh) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是汽车租赁顾问。根据用户需求从可用车辆中推荐，尽量推荐满5个方案。\n\n");
        sb.append("硬约束（必须满足）：\n");
        sb.append("1. 「N人」→ 总座位≥N。单车不够可组合多辆，用carIds数组。\n");
        sb.append("2. 「X以内」「不超过X」「预算X」→ 总价≤X。无预算则不限。\n\n");
        sb.append("软偏好（优先但不排斥其他）：\n");
        sb.append("3. 「大空间」「豪华」「省油」「SUV」等是偏好描述，优先匹配但不是硬性排除条件。\n");
        sb.append("   例如5人+大空间：7座MPV优先，但后排宽敞的5座轿车也可推荐。\n\n");
        sb.append("推荐原则：\n");
        sb.append("4. 按匹配度排序，推荐3~5个方案，覆盖不同价位、品牌、座位数，让用户有对比空间。\n");
        sb.append("5. budget内方案不够才推荐超预算的，并注明超了多少。\n");
        sb.append("6. reason写明：几辆什么车、总座位、总价、为何适合。\n");
        sb.append("7. matchScore用中文：「完美匹配」「高匹配度」「经济之选」「宽敞舒适」等。\n\n");

        if (refresh) {
            sb.append("！！！用户点了「换一批」，务必推荐与上次完全不同的方案！\n\n");
        }

        if (historyContext != null && !historyContext.isEmpty()) {
            sb.append("对话历史：\n").append(historyContext).append("\n");
            sb.append("当前是追加追问，请结合上下文理解意图。\n\n");
        }

        sb.append("返回纯JSON：{\"summary\":\"总体推荐理由\",\"recommendations\":[{\"carIds\":[1],\"reason\":\"...\",\"matchScore\":\"...\"}]}");
        return sb.toString();
    }

    /**
     * 构建用户提示词（含车辆列表和可选组合）
     */
    private String buildUserPrompt(String requirement, List<Car> availableCars, String carListStr) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("用户需求：").append(requirement).append("\n\n");
        prompt.append("可用车辆：\n").append(carListStr);

        // 当需求人数超过最大单车座位数时，预计算组合方案供AI参考
        int requiredSeats = getRequiredSeats(requirement);
        int maxSeats = availableCars.stream().mapToInt(Car::getSeats).max().orElse(0);
        if (requiredSeats > maxSeats) {
            prompt.append(buildComboOptions(availableCars, requiredSeats));
        }
        return prompt.toString();
    }

    /**
     * 预计算多车组合方案（减少AI计算量，AI负责筛选排序）
     */
    private String buildComboOptions(List<Car> availableCars, int requiredSeats) {
        StringBuilder combos = new StringBuilder();
        combos.append("\n可选组合方案（总座位≥").append(requiredSeats).append("人，已算总价，供参考）：\n");

        Set<String> seen = new HashSet<>();
        int idx = 1;

        // 同款车×N
        for (Car car : availableCars) {
            int count = (int) Math.ceil((double) requiredSeats / car.getSeats());
            if (count > 3) continue;
            int totalSeats = car.getSeats() * count;
            double totalPrice = car.getPricePerDay().doubleValue() * count;
            String key = count + "x" + car.getId();
            if (seen.add(key)) {
                combos.append(String.format("[C%d] %d辆 %s %s(%d座×%d=%d座) → ¥%.0f/天  carIds:[%d",
                        idx++, count, car.getBrand(), car.getModel(),
                        car.getSeats(), count, totalSeats, totalPrice, car.getId()));
                for (int k = 1; k < count; k++) combos.append(",").append(car.getId());
                combos.append("]\n");
            }
        }

        // 异款配对：1辆A + 1辆B
        for (int i = 0; i < availableCars.size(); i++) {
            for (int j = i + 1; j < availableCars.size(); j++) {
                Car a = availableCars.get(i), b = availableCars.get(j);
                if (a.getSeats() + b.getSeats() >= requiredSeats) {
                    int totalSeats = a.getSeats() + b.getSeats();
                    double totalPrice = a.getPricePerDay().doubleValue() + b.getPricePerDay().doubleValue();
                    String key = a.getId() + "+" + b.getId();
                    if (seen.add(key)) {
                        combos.append(String.format("[C%d] 1辆 %s %s(%d座) + 1辆 %s %s(%d座)=%d座 → ¥%.0f/天  carIds:[%d,%d]\n",
                                idx++, a.getBrand(), a.getModel(), a.getSeats(),
                                b.getBrand(), b.getModel(), b.getSeats(),
                                totalSeats, totalPrice, a.getId(), b.getId()));
                    }
                }
            }
        }
        combos.append("推荐时请引用上述carIds，组合方案务必使用carIds数组。\n");
        return combos.toString();
    }

    /**
     * 调用星火API（带重试机制）
     */
    private String callSparkWithRetry(String systemPrompt, String userPrompt) throws IOException {
        int maxRetries = 2;
        long backoffMs = 1000;
        IOException lastException = new IOException("星火API调用失败（未知原因）");

        for (int i = 0; i <= maxRetries; i++) {
            try {
                return callSpark(systemPrompt, userPrompt);
            } catch (IOException e) {
                lastException = e;
                if (i < maxRetries) {
                    log.warn("星火API调用失败，{}ms后重试({}/{})：{}", backoffMs, i + 1, maxRetries, e.getMessage());
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    backoffMs *= 2;
                }
            }
        }
        throw lastException;
    }

    /**
     * 单次调用星火API
     */
    private String callSpark(String systemPrompt, String userPrompt) throws IOException {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)));
        requestBody.put("temperature", 0.3);
        requestBody.put("max_tokens", 2000);

        String json = objectMapper.writeValueAsString(requestBody);

        // 使用星火API密码作为Bearer Token
        String auth = apiPassword;

        Request request = new Request.Builder()
                .url(baseUrl + "/chat/completions")
                .addHeader("Authorization", "Bearer " + auth)
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .post(RequestBody.create(json, MediaType.parse("application/json; charset=utf-8")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errBody = response.body() != null ? response.body().string() : "";
                log.error("星火API返回错误，状态码: {}, 响应: {}", response.code(), errBody);
                throw new IOException("星火API调用失败，状态码: " + response.code() + ", 响应: " + errBody);
            }
            // 显式按UTF-8读取响应
            byte[] respBytes = response.body().bytes();
            String body = new String(respBytes, StandardCharsets.UTF_8);
            log.debug("星火API原始响应: {}", body.length() > 500 ? body.substring(0, 500) + "..." : body);
            JsonNode root = objectMapper.readTree(body);
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.size() == 0) {
                log.error("星火API返回空choices: {}", body.length() > 300 ? body.substring(0, 300) : body);
                throw new IOException("星火API返回结果为空");
            }
            String content = choices.get(0).path("message").path("content").asText();
            if (content.isEmpty()) {
                log.error("星火API返回空content: {}", body.length() > 300 ? body.substring(0, 300) : body);
                throw new IOException("星火API返回内容为空");
            }
            log.debug("星火API提取内容长度: {}", content.length());
            return content;
        }
    }

    // ==================== 结果解析 ====================

    /**
     * 解析AI返回的JSON推荐结果，支持carId(旧格式)和carIds(新格式)
     */
    private AIRecommendResult parseRecommendResult(String response, List<Car> availableCars, String originalReq) {
        try {
            // 清理可能的markdown包裹
            String json = response.trim();
            if (json.startsWith("```")) {
                json = json.replaceAll("```json?", "").replaceAll("```", "").trim();
            }
            log.debug("解析AI响应JSON, 长度={}", json.length());

            Map<Long, Car> carMap = availableCars.stream()
                    .collect(Collectors.toMap(Car::getId, c -> c, (a, b) -> a));

            JsonNode root = objectMapper.readTree(json);
            AIRecommendResult result = new AIRecommendResult();
            result.setSummary(root.path("summary").asText("根据您的需求，为您推荐以下车型"));

            List<AIRecommendResult.RecommendItem> items = new ArrayList<>();
            JsonNode recs = root.path("recommendations");
            for (JsonNode rec : recs) {
                List<Car> matchedCars = new ArrayList<>();

                // 优先解析 carIds 数组（新格式）
                JsonNode carIdsNode = rec.path("carIds");
                if (carIdsNode.isArray() && carIdsNode.size() > 0) {
                    for (JsonNode idNode : carIdsNode) {
                        Long carId = idNode.asLong();
                        Car car = carMap.get(carId);
                        if (car != null) {
                            matchedCars.add(car);
                        } else {
                            log.warn("AI返回未知carId: {}", carId);
                        }
                    }
                } else {
                    // 兼容旧格式 carId（单个数字）
                    long carId = rec.path("carId").asLong(0);
                    if (carId > 0) {
                        Car car = carMap.get(carId);
                        if (car != null) {
                            matchedCars.add(car);
                        }
                    }
                }

                if (!matchedCars.isEmpty()) {
                    AIRecommendResult.RecommendItem item = new AIRecommendResult.RecommendItem();
                    item.setCars(matchedCars);
                    item.setReason(rec.path("reason").asText(""));
                    item.setMatchScore(rec.path("matchScore").asText(""));
                    items.add(item);
                }
            }

            // 如果AI没有返回有效推荐，降级
            if (items.isEmpty()) {
                log.warn("AI未返回有效推荐项，降级为本地推荐");
                return fallbackRecommend(originalReq, availableCars, "");
            }

            result.setRecommendations(items);
            return result;
        } catch (Exception e) {
            log.error("解析AI推荐结果失败: {}", e.getMessage());
            return fallbackRecommend(originalReq, availableCars, "");
        }
    }

    // ==================== 多轮对话 ====================

    /**
     * 构建对话历史上下文文本
     */
    private String buildHistoryContext(String conversationId) {
        if (conversationId == null || conversationId.isEmpty()) return "";

        List<Map<String, String>> history = conversationStore.get(conversationId);
        if (history == null || history.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        for (Map<String, String> turn : history) {
            sb.append("用户：").append(turn.get("user")).append("\n");
            sb.append("助手：").append(turn.get("assistant")).append("\n");
        }
        log.debug("加载对话历史, conversationId={}, 轮数={}", conversationId, history.size() / 2);
        return sb.toString();
    }

    /**
     * 保存对话记录，返回新的conversationId
     */
    private String saveConversation(String existingId, String userMsg, String aiSummary) {
        String convId = (existingId != null && !existingId.isEmpty())
                ? existingId
                : UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        List<Map<String, String>> history = conversationStore.computeIfAbsent(convId, k -> new ArrayList<>());

        Map<String, String> turn = new HashMap<>();
        turn.put("user", userMsg.length() > 500 ? userMsg.substring(0, 500) + "..." : userMsg);
        turn.put("assistant", aiSummary != null ? aiSummary : "");
        history.add(turn);

        // 限制历史轮数
        while (history.size() > MAX_HISTORY_TURNS * 2) {
            history.remove(0);
        }

        log.debug("保存对话, conversationId={}, 当前轮数={}", convId, history.size());
        return convId;
    }

    // ==================== 本地兜底 ====================

    /**
     * 本地关键词推荐（仅当AI API完全不可用时兜底）
     * @param requirement 用户当前输入
     * @param cars 可用车辆列表
     * @param historyContext 对话历史上下文（可为空）
     */
    private AIRecommendResult fallbackRecommend(String requirement, List<Car> cars, String historyContext) {
        // 如果有对话历史且当前是短追问，合并上下文
        String effectiveReq = requirement != null ? requirement : "";
        if (historyContext != null && !historyContext.isEmpty() && effectiveReq.length() < 30) {
            log.info("追问检测：合并对话上下文进行本地分析, 当前=\"{}\"", effectiveReq);
            effectiveReq = extractLastUserRequirement(historyContext) + "。" + effectiveReq;
            log.info("合并后需求: \"{}\"", effectiveReq.length() > 100 ? effectiveReq.substring(0, 100) + "..." : effectiveReq);
        }
        log.info("执行本地兜底推荐, 有效需求=\"{}\", 车辆数={}", effectiveReq, cars.size());
        AIRecommendResult result = new AIRecommendResult();

        String req = effectiveReq.toLowerCase();
        int requiredSeats = getRequiredSeats(req);

        // 先按座位数过滤
        List<Car> filtered = new ArrayList<>();
        for (Car car : cars) {
            if (requiredSeats <= 0 || car.getSeats() >= requiredSeats) {
                filtered.add(car);
            }
        }

        // 如果过滤后没有车 → 多车组合建议
        if (filtered.isEmpty()) {
            return buildMultiCarSuggestion(cars, requiredSeats, req);
        }

        List<Car> scored = new ArrayList<>(filtered);
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
            item.setCars(List.of(car));
            item.setReason(buildReason(car, req));
            item.setMatchScore(calcMatchScore(car, req, reqSeats) + "%");
            items.add(item);
        }

        result.setSummary(String.format("为您从 %d 辆可用车辆中推荐了 %d 辆最匹配的车型（本地推荐）", filtered.size(), items.size()));
        result.setRecommendations(items);
        return result;
    }

    // ==================== 辅助方法 ====================

    /**
     * 从对话历史文本中提取最近一条用户消息
     */
    private String extractLastUserRequirement(String historyContext) {
        if (historyContext == null || historyContext.isEmpty()) return "";
        // 格式：用户：xxx\n助手：xxx\n用户：xxx\n...
        String[] lines = historyContext.split("\n");
        String lastUser = "";
        for (String line : lines) {
            if (line.startsWith("用户：")) {
                lastUser = line.substring(3).trim();
            }
        }
        return lastUser;
    }

    /**
     * 从需求中提取所需座位数，返回0表示未明确指定
     */
    private int getRequiredSeats(String req) {
        if (req == null) return 0;
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

    /**
     * 从需求中提取预算金额，返回0表示未指定
     */
    private int getBudget(String req) {
        if (req == null) return 0;
        java.util.regex.Pattern p;
        java.util.regex.Matcher m;
        // "预算500"、"预算 500"
        p = java.util.regex.Pattern.compile("预算\\s*(\\d+)");
        m = p.matcher(req);
        if (m.find()) return Integer.parseInt(m.group(1));
        // "500元/天"、"500/天"
        p = java.util.regex.Pattern.compile("(\\d+)\\s*元?/天");
        m = p.matcher(req);
        if (m.find()) return Integer.parseInt(m.group(1));
        // "不超过500"、"不大于500"、"不高于500"
        p = java.util.regex.Pattern.compile("(?:不超过|不大于|不高于)\\s*(\\d+)");
        m = p.matcher(req);
        if (m.find()) return Integer.parseInt(m.group(1));
        // "500以内"、"500以下"、"500之内"
        p = java.util.regex.Pattern.compile("(\\d+)\\s*(?:以内|以下|之内)");
        m = p.matcher(req);
        if (m.find()) return Integer.parseInt(m.group(1));
        // "500左右"
        p = java.util.regex.Pattern.compile("(\\d+)\\s*左右");
        m = p.matcher(req);
        if (m.find()) return Integer.parseInt(m.group(1));
        return 0;
    }

    /**
     * 人数超出所有车辆座位数时，枚举多车组合方案
     */
    private AIRecommendResult buildMultiCarSuggestion(List<Car> cars, int requiredSeats, String req) {
        AIRecommendResult result = new AIRecommendResult();
        int budget = getBudget(req);
        List<AIRecommendResult.RecommendItem> items = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        // 1. 同款车 × N
        for (Car car : cars) {
            int count = (int) Math.ceil((double) requiredSeats / car.getSeats());
            if (count > 3) continue;
            double totalCost = car.getPricePerDay().doubleValue() * count;
            String key = count + "x" + car.getId();
            if (seen.add(key)) {
                items.add(buildComboItem(List.of(car), count, totalCost, budget));
            }
        }

        // 2. 异款配对：1辆A + 1辆B
        for (int i = 0; i < cars.size(); i++) {
            for (int j = i + 1; j < cars.size(); j++) {
                Car a = cars.get(i), b = cars.get(j);
                if (a.getSeats() + b.getSeats() >= requiredSeats) {
                    double totalCost = a.getPricePerDay().doubleValue() + b.getPricePerDay().doubleValue();
                    String key = a.getId() + "+" + b.getId();
                    if (seen.add(key)) {
                        items.add(buildComboItem(List.of(a, b), 1, totalCost, budget));
                    }
                }
            }
        }

        // 按总价排序
        items.sort((x, y) -> {
            double px = parsePrice(x.getMatchScore());
            double py = parsePrice(y.getMatchScore());
            return Double.compare(px, py);
        });

        // 预算内优先，最多返回5条
        List<AIRecommendResult.RecommendItem> within = new ArrayList<>();
        List<AIRecommendResult.RecommendItem> over = new ArrayList<>();
        for (AIRecommendResult.RecommendItem item : items) {
            double price = parsePrice(item.getMatchScore());
            if (budget > 0 && price > budget) {
                if (over.size() < 3) over.add(item);
            } else {
                if (within.size() < 5) within.add(item);
            }
        }

        List<AIRecommendResult.RecommendItem> finalList = within.isEmpty() ? over : within;
        if (!within.isEmpty() && !over.isEmpty()) {
            finalList = new ArrayList<>(within);
        }

        // 最便宜标最划算
        if (!finalList.isEmpty()) {
            AIRecommendResult.RecommendItem best = finalList.get(0);
            best.setReason("[最划算] " + best.getReason());
            best.setMatchScore(best.getMatchScore() + " [最划算]");
        }

        String summary;
        if (budget > 0 && within.isEmpty()) {
            summary = String.format("当前无%d座车辆，预算¥%d/天以内无可行方案，以下为最接近的方案（均超预算）：", requiredSeats, budget);
        } else if (budget > 0) {
            summary = String.format("当前无%d座车辆，为您列出预算内组合方案（≤¥%d/天）：", requiredSeats, budget);
        } else {
            summary = String.format("当前无%d座车辆，为您列出 %d 种组合方案，按总价排序：", requiredSeats, finalList.size());
        }
        result.setSummary(summary);
        result.setRecommendations(finalList);
        return result;
    }

    /** 构建一条组合推荐 */
    private AIRecommendResult.RecommendItem buildComboItem(List<Car> carList, int multiplier,
                                                            double totalCost, int budget) {
        AIRecommendResult.RecommendItem item = new AIRecommendResult.RecommendItem();

        // 展开同款多辆
        List<Car> expanded = new ArrayList<>();
        for (Car c : carList) {
            for (int k = 0; k < multiplier; k++) {
                expanded.add(c);
            }
        }
        // 对于异款配对，carList已有2个不同元素，multiplier=1，无需展开
        item.setCars(expanded.size() >= carList.size() ? expanded : carList);

        int totalSeats = expanded.stream().mapToInt(Car::getSeats).sum();
        String tag = (budget > 0 && totalCost > budget) ? " [超预算]" : "";

        StringBuilder reason = new StringBuilder();
        if (carList.size() == 1) {
            Car c = carList.get(0);
            reason.append(String.format("%d辆%s%s（%d座×%d=%d座），总价¥%.0f/天%s",
                    multiplier, c.getBrand(), c.getModel(),
                    c.getSeats(), multiplier, totalSeats, totalCost, tag));
        } else {
            Car a = carList.get(0), b = carList.get(1);
            reason.append(String.format("1辆%s%s（%d座）+ 1辆%s%s（%d座）=%d座，总价¥%.0f/天%s",
                    a.getBrand(), a.getModel(), a.getSeats(),
                    b.getBrand(), b.getModel(), b.getSeats(),
                    totalSeats, totalCost, tag));
        }
        item.setReason(reason.toString());
        item.setMatchScore(String.format("¥%.0f/天", totalCost));
        return item;
    }

    private double parsePrice(String matchScore) {
        if (matchScore == null) return 99999;
        try {
            return Double.parseDouble(matchScore.replace("¥", "").replace("/天", "").trim());
        } catch (NumberFormatException e) {
            return 99999;
        }
    }

    private int calcMatchScore(Car car, String req, int requiredSeats) {
        int score = 50;

        if (requiredSeats > 0) {
            if (car.getSeats() >= requiredSeats + 1) {
                score += 35;
            } else if (car.getSeats() >= requiredSeats) {
                score += 30;
            }
        }

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

        if ((req.contains("suv") || req.contains("越野")) && "SUV".equals(car.getCategory())) score += 20;
        if ((req.contains("商务") || req.contains("接待")) && "MPV".equals(car.getCategory())) score += 20;
        if ((req.contains("电车") || req.contains("纯电") || req.contains("新能源")) && "新能源".equals(car.getCategory())) score += 20;
        if ((req.contains("轿车") || req.contains("通勤")) && "轿车".equals(car.getCategory())) score += 15;

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
