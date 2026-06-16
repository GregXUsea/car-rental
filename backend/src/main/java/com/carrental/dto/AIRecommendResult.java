package com.carrental.dto;

import com.carrental.entity.Car;
import lombok.Data;
import java.util.List;

@Data
public class AIRecommendResult {
    private String summary;
    private String poweredBy;
    private List<RecommendItem> recommendations;

    @Data
    public static class RecommendItem {
        private Car car;
        private String reason;
        private String matchScore;
    }
}
