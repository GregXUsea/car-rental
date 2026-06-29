package com.carrental.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("drivers")
public class Driver {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String phone;

    private String idCard;

    private String licenseType;

    private LocalDate licenseExpireDate;

    private Integer experienceYears;

    private String avatar;

    private Integer status;  // 0空闲 1服务中 2休假 3离职

    private BigDecimal rating;

    private Integer serviceCount;

    private String description;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
