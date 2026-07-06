package com.carrental.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 消息实体 - 管理员与用户沟通
 */
@Data
@TableName("messages")
public class Message {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long senderId;      // 发送者ID
    private Long receiverId;    // 接收者ID
    private String content;     // 消息内容
    private Integer isRead;     // 0未读 1已读
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    // 非数据库字段
    @TableField(exist = false)
    private String senderName;
    @TableField(exist = false)
    private String receiverName;
}
