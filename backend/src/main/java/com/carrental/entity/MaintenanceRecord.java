package com.carrental.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@TableName("maintenance_records")
public class MaintenanceRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long carId;
    private Integer mileageAtMaintenance;
    private String maintenanceType;
    private String description;
    private BigDecimal cost;
    private LocalDate maintenanceDate;
}
