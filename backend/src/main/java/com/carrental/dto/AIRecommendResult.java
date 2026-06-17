package com.carrental.dto;

import com.carrental.entity.Car;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class AIRecommendResult {
    private String summary;
    private String poweredBy;
    private String conversationId;
    private List<RecommendItem> recommendations;

    @Data
    public static class RecommendItem {
        /** 推荐车辆列表，单车推荐含1个元素，多车组合含多个元素 */
        private List<Car> cars = new ArrayList<>();
        private String reason;
        private String matchScore;

        /** 兼容旧前端：返回第一辆车 */
        @JsonProperty("car")
        public Car getCar() {
            return (cars != null && !cars.isEmpty()) ? cars.get(0) : null;
        }
    }
}
