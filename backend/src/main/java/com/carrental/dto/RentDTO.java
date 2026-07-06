package com.carrental.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RentDTO {
    @NotNull(message = "车辆ID不能为空")
    private Long carId;
    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;
    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;
    private Boolean isReservation;
    private Long driverId;
    private String remark;
    private Integer startMileage;
    private Long pickupStoreId;  // 取车门店ID
    private Long returnStoreId;  // 还车门店ID
    private String pickupCity;   // 取车城市
    private String returnCity;   // 还车城市
}
