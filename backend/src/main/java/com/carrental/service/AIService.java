package com.carrental.service;

import com.carrental.dto.AIRecommendResult;
import com.carrental.entity.Car;
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

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();

    /** 多轮对话上下文存储：conversationId → 历史消息列表 */
    private final Map<String, List<Map<String, String>>> conversationStore = new ConcurrentHashMap<>();
    /** 追踪每轮对话已推荐的车辆ID集合，用于告知AI避开 */
    private final Map<String, Set<Long>> pastRecommendations = new ConcurrentHashMap<>();
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

        // 构建对话历史上下文 + 获取已推荐的车辆ID
        String historyContext = buildHistoryContext(conversationId);
        Set<Long> pastCarIds = conversationId != null ? pastRecommendations.getOrDefault(conversationId, Collections.emptySet()) : Collections.emptySet();

        // API 密码未配置时直接报错，不做本地兜底
        if (apiPassword == null || apiPassword.isBlank()) {
            log.error("星火API密码未配置，无法使用AI推荐");
            throw new RuntimeException("AI推荐服务未配置，请联系管理员设置星火API密码");
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
        String systemPrompt = buildSystemPrompt(historyContext, refresh, pastCarIds);

        // 构建用户提示词（需求 + 车辆列表 + 可选组合）
        String userPrompt = buildUserPrompt(originalReq, availableCars, carListStr.toString());

        try {
            log.info("调用星火API, model={}, prompt总长度={}", model, userPrompt.length());
            String response = callSparkWithRetry(systemPrompt, userPrompt);
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("星火API响应耗时: {}ms", elapsed);

            AIRecommendResult aiResult = parseRecommendResult(response, availableCars, originalReq);
            aiResult.setPoweredBy("AI");

            // 程序级兜底：换一批时过滤掉已推荐过的车辆（真正从结果中移除）
            Set<Long> allPastIds = conversationId != null ? pastRecommendations.getOrDefault(conversationId, Collections.emptySet()) : Collections.emptySet();
            if (refresh && !allPastIds.isEmpty() && aiResult.getRecommendations() != null) {
                List<AIRecommendResult.RecommendItem> freshItems = new ArrayList<>();
                Set<Long> newCarIds = new HashSet<>();
                for (AIRecommendResult.RecommendItem item : aiResult.getRecommendations()) {
                    if (item.getCars() == null || item.getCars().isEmpty()) continue;
                    // 真正从item中移除已推荐过的车辆
                    List<Car> freshCars = item.getCars().stream()
                            .filter(c -> !allPastIds.contains(c.getId()))
                            .collect(Collectors.toList());
                    if (!freshCars.isEmpty()) {
                        item.setCars(freshCars);
                        freshItems.add(item);
                        freshCars.forEach(c -> newCarIds.add(c.getId()));
                    }
                }
                if (freshItems.isEmpty()) {
                    log.info("换一批：所有方案均与上批重复，返回暂无其他方案");
                    AIRecommendResult noMore = new AIRecommendResult();
                    noMore.setSummary("暂无其他方案——当前可选车辆中，除已推荐的外没有其他匹配方案。您可以调整预算或需求条件后重试。");
                    noMore.setPoweredBy("AI");
                    noMore.setConversationId(conversationId);
                    noMore.setRecommendations(Collections.emptyList());
                    return noMore;
                }
                aiResult.setRecommendations(freshItems);
                log.info("换一批去重: 过滤前{}项 → 过滤后{}项, 已推荐IDs={}",
                        aiResult.getRecommendations().size() + (aiResult.getRecommendations().size() - freshItems.size()),
                        freshItems.size(), allPastIds);
                String newConvId = saveConversation(conversationId, originalReq, aiResult.getSummary(), newCarIds);
                aiResult.setConversationId(newConvId);
            } else {
                // 提取本次推荐所有车辆ID并保存
                Set<Long> thisCarIds = new HashSet<>();
                if (aiResult.getRecommendations() != null) {
                    for (AIRecommendResult.RecommendItem item : aiResult.getRecommendations()) {
                        if (item.getCars() != null) {
                            item.getCars().forEach(c -> thisCarIds.add(c.getId()));
                        }
                    }
                }
                String newConvId = saveConversation(conversationId, originalReq, aiResult.getSummary(), thisCarIds);
                aiResult.setConversationId(newConvId);
            }

            return aiResult;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("星火API调用失败(耗时{}ms): {}", elapsed, e.getMessage());
            throw new RuntimeException("AI推荐服务暂时不可用，请稍后重试: " + e.getMessage());
        }
    }

    // ==================== AI调用核心 ====================

    /**
     * 构建系统提示词
     */
    private String buildSystemPrompt(String historyContext, boolean refresh, Set<Long> pastCarIds) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是汽车租赁顾问。根据用户需求从可用车辆中推荐最匹配的方案。\n\n");
        sb.append("硬约束（必须满足）：\n");
        sb.append("1. 「N人」→ 总座位≥N。仅当用户明确需要多人出行且单车座位不足时，才推荐多车组合。\n");
        sb.append("2. 「预算X」「X以内」「不超过X」「X内」「降到X」→ 总价严格≤X，绝对不能超。无预算则不限。\n");
        sb.append("3. 商务接待、个人代步、通勤等单人/少数人场景，只推荐单车方案，禁止拼凑多车组合。\n\n");
        sb.append("软偏好（优先但不排斥其他）：\n");
        sb.append("3. 「大空间」「豪华」「省油」「SUV」等是偏好描述，优先匹配但不是硬性排除条件。\n\n");
        sb.append("推荐原则：\n");
        sb.append("4. 按匹配度排序，尽量推荐3~5个方案覆盖不同价位和品牌；匹配度不够时2个也行，不要凑不满足条件的。\n");
        sb.append("5. reason写明：什么车、总座位、总价、为何适合。\n");
        sb.append("6. matchScore用中文：「完美匹配」「高匹配度」「经济之选」「宽敞舒适」等。\n\n");

        // 已推荐过的车辆ID列表，让AI避开
        if (refresh && pastCarIds != null && !pastCarIds.isEmpty()) {
            sb.append("！！！用户点了「换一批」，以下车辆ID已在上批推荐过，请务必从可用车辆中排除这些ID：");
            sb.append(pastCarIds.stream().map(String::valueOf).collect(Collectors.joining(", ")));
            sb.append("\n");
            sb.append("若排除后无其他匹配方案，summary中如实说明\"暂无其他方案\"，recommendations返回空数组[]。\n\n");
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
        requestBody.put("thinking", Map.of("type", "disabled"));  // 禁用深度思考，避免token全耗在reasoning_content上

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

            // AI没有返回有效推荐
            if (items.isEmpty()) {
                log.warn("AI未返回有效推荐项");
                AIRecommendResult emptyResult = new AIRecommendResult();
                emptyResult.setSummary("AI暂未找到匹配的车辆方案，请尝试调整需求条件后重试。");
                emptyResult.setPoweredBy("AI");
                emptyResult.setRecommendations(Collections.emptyList());
                return emptyResult;
            }

            result.setRecommendations(items);
            return result;
        } catch (Exception e) {
            log.error("解析AI推荐结果失败: {}", e.getMessage());
            AIRecommendResult errorResult = new AIRecommendResult();
            errorResult.setSummary("AI返回结果解析失败，请重试或调整需求描述。");
            errorResult.setPoweredBy("AI");
            errorResult.setRecommendations(Collections.emptyList());
            return errorResult;
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
     * 保存对话记录和已推荐车辆ID，返回新的conversationId
     */
    private String saveConversation(String existingId, String userMsg, String aiSummary, Set<Long> carIds) {
        String convId = (existingId != null && !existingId.isEmpty())
                ? existingId
                : UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        List<Map<String, String>> history = conversationStore.computeIfAbsent(convId, k -> new ArrayList<>());

        Map<String, String> turn = new HashMap<>();
        turn.put("user", userMsg.length() > 500 ? userMsg.substring(0, 500) + "..." : userMsg);
        turn.put("assistant", aiSummary != null ? aiSummary : "");
        history.add(turn);

        // 记录已推荐车辆ID（合并而非覆盖，支持多轮累积）
        if (carIds != null && !carIds.isEmpty()) {
            pastRecommendations.computeIfAbsent(convId, k -> new HashSet<>()).addAll(carIds);
        }

        // 限制历史轮数
        while (history.size() > MAX_HISTORY_TURNS * 2) {
            history.remove(0);
        }

        log.debug("保存对话, conversationId={}, 当前轮数={}", convId, history.size());
        return convId;
    }

    // ==================== 辅助方法 ====================

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

}
