package com.carrental.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券实体
 */
@Data
@TableName("coupons")
public class Coupon {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String couponCode;
    private Integer couponType; // 1立减券 2折扣券
    private BigDecimal discountAmount; // 立减金额
    private BigDecimal discountRate; // 折扣率 0.5=5折
    private BigDecimal minAmount; // 最低消费
    private Integer status; // 0未使用 1已使用 2已过期
    private LocalDateTime expireTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
