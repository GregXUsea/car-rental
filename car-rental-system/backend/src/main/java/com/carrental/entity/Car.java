package com.carrental.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("cars")
public class Car {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String brand;
    private String model;
    private String color;
    private Integer seats;
    private BigDecimal pricePerDay;
    private BigDecimal deposit;
    private String image;
    private Integer status;
    private Integer mileage;
    private LocalDate lastMaintainDate;
    private String description;
    private String category;
    private String usageType;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
