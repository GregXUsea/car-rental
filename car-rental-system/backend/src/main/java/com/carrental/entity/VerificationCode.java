package com.carrental.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("verification_codes")
public class VerificationCode {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String email;
    private String code;
    private Integer used;
    private LocalDateTime createTime;
    private LocalDateTime expireTime;
}
