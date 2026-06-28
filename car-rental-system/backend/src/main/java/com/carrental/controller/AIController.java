package com.carrental.controller;

import com.carrental.dto.AIRecommendRequest;
import com.carrental.dto.AIRecommendResult;
import com.carrental.dto.Result;
import com.carrental.service.AIService;
import com.carrental.service.MaintenancePredictionCache;
import com.carrental.service.RAGService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AIController {

    @Autowired
    private AIService aiService;

    @Autowired
    private RAGService ragService;

    @Autowired
    private MaintenancePredictionCache predictionCache;

    @PostMapping("/recommend")
    public Result<AIRecommendResult> recommend(@RequestBody AIRecommendRequest request) {
        try {
            AIRecommendResult result = aiService.recommendCars(request.getRequirement());
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("AI推荐服务暂时不可用: " + e.getMessage());
        }
    }

    /**
     * 通用AI对话（支持租车推荐 + 普通问答 + 上下文记忆）
     */
    @SuppressWarnings("unchecked")
    @PostMapping("/chat")
    public Result<Map<String, Object>> chat(@RequestBody java.util.Map<String, Object> request) {
        try {
            String message = (String) request.get("message");
            if (message == null || message.isBlank()) {
                return Result.error("请输入消息内容");
            }
            // 获取对话历史
            java.util.List<java.util.Map<String, String>> history = null;
            Object historyObj = request.get("history");
            if (historyObj instanceof java.util.List) {
                history = (java.util.List<java.util.Map<String, String>>) historyObj;
            }
            return Result.success(aiService.chat(message, history));
        } catch (Exception e) {
            return Result.error("AI服务暂时不可用: " + e.getMessage());
        }
    }

    @GetMapping("/maintenance/{carId}")
    public Result<Map<String, Object>> maintenancePrediction(@PathVariable Long carId) {
        try {
            return Result.success(predictionCache.getPrediction(carId));
        } catch (Exception e) {
            return Result.error("维护预测服务暂时不可用: " + e.getMessage());
        }
    }

    /**
     * 批量获取所有车辆的AI维护预测（优先读缓存，秒开）
     */
    @GetMapping("/maintenance/all")
    public Result<List<Map<String, Object>>> maintenancePredictionAll() {
        try {
            List<Map<String, Object>> cached = predictionCache.getAllPredictions();
            if (!cached.isEmpty()) {
                return Result.success(cached);
            }
            // 缓存为空时同步刷新（快速预测，毫秒级）
            predictionCache.refreshAll();
            return Result.success(predictionCache.getAllPredictions());
        } catch (Exception e) {
            return Result.error("批量维护预测失败: " + e.getMessage());
        }
    }

    /**
     * 手动触发全量预测刷新
     * 可选参数: ?ai=true (默认使用当前模式)
     */
    @PostMapping("/maintenance/refresh")
    public Result<Map<String, Object>> refreshPredictions(
            @RequestParam(defaultValue = "") String mode) {
        try {
            if ("ai".equals(mode)) {
                return Result.success(predictionCache.refreshAll(true));
            } else if ("local".equals(mode)) {
                return Result.success(predictionCache.refreshAll(false));
            }
            return Result.success(predictionCache.refreshAll());
        } catch (Exception e) {
            return Result.error("刷新失败: " + e.getMessage());
        }
    }

    /**
     * 切换AI/本地预测模式
     */
    @PostMapping("/maintenance/mode")
    public Result<Map<String, Object>> switchMode(@RequestBody Map<String, Boolean> body) {
        boolean aiMode = Boolean.TRUE.equals(body.get("aiMode"));
        predictionCache.setAiMode(aiMode);
        Map<String, Object> result = new HashMap<>();
        result.put("aiMode", aiMode);
        result.put("message", aiMode ? "已切换到AI模式 (DeepSeek API)" : "已切换到本地模式 (规则引擎)");
        return Result.success(result);
    }

    /**
     * 获取预测缓存状态
     */
    @GetMapping("/maintenance/cache-status")
    public Result<Map<String, Object>> cacheStatus() {
        return Result.success(predictionCache.getStatus());
    }

    /**
     * 获取RAG知识库状态
     */
    @GetMapping("/rag/status")
    public Result<Map<String, Object>> ragStatus() {
        return Result.success(ragService.getStatus());
    }

    /**
     * 手动触发RAG知识库重建
     */
    @PostMapping("/rag/rebuild")
    public Result<Map<String, Object>> ragRebuild() {
        try {
            return Result.success(ragService.rebuildKnowledgeBase());
        } catch (Exception e) {
            return Result.error("知识库重建失败: " + e.getMessage());
        }
    }
}
