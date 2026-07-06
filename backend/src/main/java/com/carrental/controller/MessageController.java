package com.carrental.controller;

import com.carrental.dto.Result;
import com.carrental.entity.Message;
import com.carrental.entity.User;
import com.carrental.mapper.UserMapper;
import com.carrental.service.MessageService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 消息控制器 - 管理员与用户沟通
 */
@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserMapper userMapper;

    /**
     * 获取管理员ID（供用户发消息用）
     */
    @GetMapping("/admin-id")
    public Result<Long> getAdminId() {
        User admin = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getRole, 1).last("LIMIT 1")
        );
        if (admin == null) return Result.error("未找到管理员");
        return Result.success(admin.getId());
    }

    /**
     * 发送消息
     */
    @PostMapping("/send")
    public Result<Message> sendMessage(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long senderId = (Long) request.getAttribute("userId");
        Long receiverId = Long.valueOf(body.get("receiverId").toString());
        String content = (String) body.get("content");

        if (content == null || content.trim().isEmpty()) {
            return Result.error("消息内容不能为空");
        }

        Message msg = messageService.sendMessage(senderId, receiverId, content.trim());
        return Result.success(msg);
    }

    /**
     * 获取与某用户的对话
     */
    @GetMapping("/conversation/{userId}")
    public Result<List<Message>> getConversation(@PathVariable Long userId, HttpServletRequest request) {
        Long currentUserId = (Long) request.getAttribute("userId");
        List<Message> messages = messageService.getConversation(currentUserId, userId);

        // 标记对方发来的消息为已读
        messageService.markAllAsRead(userId, currentUserId);

        return Result.success(messages);
    }

    /**
     * 获取对话列表（管理员用）
     */
    @GetMapping("/conversations")
    public Result<List<Map<String, Object>>> getConversationList(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<Map<String, Object>> conversations = messageService.getConversationList(userId);
        return Result.success(conversations);
    }

    /**
     * 获取未读消息数
     */
    @GetMapping("/unread")
    public Result<Long> getUnreadCount(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        long count = messageService.getUnreadCount(userId);
        return Result.success(count);
    }

    /**
     * 标记消息已读
     */
    @PutMapping("/read/{id}")
    public Result<Void> markAsRead(@PathVariable Long id) {
        messageService.markAsRead(id);
        return Result.success(null);
    }
}
