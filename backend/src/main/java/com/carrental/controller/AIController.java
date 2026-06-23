package com.carrental.controller;

import com.carrental.dto.AIRecommendRequest;
import com.carrental.dto.AIRecommendResult;
import com.carrental.dto.Result;
import com.carrental.service.AIService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AIController {

    @Autowired
    private AIService aiService;

    @PostMapping("/recommend")
    public Result<AIRecommendResult> recommend(@Valid @RequestBody AIRecommendRequest request) {
        try {
            AIRecommendResult result = aiService.recommendCars(
                    request.getRequirement(), request.getConversationId(),
                    request.getRefresh() != null && request.getRefresh());
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("AI推荐服务暂时不可用: " + e.getMessage());
        }
    }

}
