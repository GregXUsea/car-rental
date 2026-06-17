package com.carrental.controller;

import com.carrental.dto.AIRecommendRequest;
import com.carrental.dto.AIRecommendResult;
import com.carrental.dto.Result;
import com.carrental.service.AIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AIController {

    @Autowired
    private AIService aiService;

    @PostMapping("/recommend")
    public Result<AIRecommendResult> recommend(@RequestBody AIRecommendRequest request) {
        try {
            AIRecommendResult result = aiService.recommendCars(
                    request.getRequirement(), request.getConversationId());
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("AI推荐服务暂时不可用: " + e.getMessage());
        }
    }

    @GetMapping("/maintenance/{carId}")
    public Result<Map<String, Object>> maintenancePrediction(@PathVariable Long carId) {
        try {
            return Result.success(aiService.getMaintenancePrediction(carId));
        } catch (Exception e) {
            return Result.error("维护预测服务暂时不可用: " + e.getMessage());
        }
    }
}
