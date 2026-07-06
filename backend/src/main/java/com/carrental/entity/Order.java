package com.carrental.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("orders")
public class Order {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long userId;
    private Long carId;
    private Long driverId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime actualReturnTime;
    private BigDecimal totalCost;
    private BigDecimal driverCost;
    private BigDecimal deposit;
    private BigDecimal depositRefund;
    private Integer status;
    private String remark;
    private Integer userRating;
    private String userComment;
    private Integer startMileage;
    private Integer endMileage;
    private Integer mileageDriven;
    private BigDecimal discount;
    private Integer depositPaid;
    private Integer rentalPaid;
    private LocalDateTime depositPaidTime;
    private LocalDateTime rentalPaidTime;
    private LocalDateTime cancelTime;
    private Integer pickupConfirmed; // 0未确认 1已确认取车
    private LocalDateTime pickupTime; // 确认取车时间
    private Integer pickupWarningSent; // 0未发送 1已发送2h警告
    private Long pickupStoreId;     // 取车门店ID
    private Long returnStoreId;     // 还车门店ID
    private String pickupCity;      // 取车城市
    private String returnCity;      // 还车城市
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(exist = false)
    private Car car;
    @TableField(exist = false)
    private Driver driver;
    @TableField(exist = false)
    private String username;
    @TableField(exist = false)
    private Store pickupStore;      // 取车门店
    @TableField(exist = false)
    private Store returnStore;      // 还车门店
}
