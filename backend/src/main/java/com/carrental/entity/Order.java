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
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime actualReturnTime;
    private BigDecimal totalCost;
    private BigDecimal deposit;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(exist = false)
    private Car car;
    @TableField(exist = false)
    private String username;
}
