package com.carrental.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 门店实体
 */
@Data
@TableName("stores")
public class Store {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;          // 门店名称
    private String city;          // 城市
    private String address;       // 详细地址
    private String phone;         // 联系电话
    private BigDecimal longitude; // 经度
    private BigDecimal latitude;  // 纬度
    private Integer status;       // 0关闭 1营业中 2休息
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
