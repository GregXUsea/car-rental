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
import java.util.concurrent.ConcurrentHashMap;

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

    // 输入状态存储（临时存储，生产环境应使用Redis）
    private static final ConcurrentHashMap<Long, Long> typingUsers = new ConcurrentHashMap<>();
    // 在线状态存储（用户最后活跃时间）
    private static final ConcurrentHashMap<Long, Long> onlineUsers = new ConcurrentHashMap<>();

    /**
     * 发送"正在输入"状态
     */
    @PostMapping("/typing")
    public Result<Void> sendTyping(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Long receiverId = Long.valueOf(body.get("receiverId").toString());
        // 记录输入状态，3秒后自动过期
        typingUsers.put(userId, System.currentTimeMillis());
        // 同时更新在线状态
        onlineUsers.put(userId, System.currentTimeMillis());
        // 3秒后自动清除输入状态
        new Thread(() -> {
            try { Thread.sleep(3000); } catch (InterruptedException e) {}
            Long lastTime = typingUsers.get(userId);
            if (lastTime != null && System.currentTimeMillis() - lastTime >= 2900) {
                typingUsers.remove(userId);
            }
        }).start();
        return Result.success(null);
    }

    /**
     * 查询对方是否正在输入
     */
    @GetMapping("/typing-status/{targetUserId}")
    public Result<Boolean> getTypingStatus(@PathVariable Long targetUserId) {
        Long lastTime = typingUsers.get(targetUserId);
        boolean isTyping = lastTime != null && (System.currentTimeMillis() - lastTime) < 3000;
        return Result.success(isTyping);
    }

    /**
     * 心跳：用户定期调用，更新在线状态
     */
    @PostMapping("/heartbeat")
    public Result<Void> heartbeat(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        onlineUsers.put(userId, System.currentTimeMillis());
        return Result.success(null);
    }

    /**
     * 查询对方是否在线（30秒内有活跃）
     */
    @GetMapping("/online-status/{targetUserId}")
    public Result<Boolean> getOnlineStatus(@PathVariable Long targetUserId) {
        Long lastTime = onlineUsers.get(targetUserId);
        boolean isOnline = lastTime != null && (System.currentTimeMillis() - lastTime) < 30000; // 30秒内活跃
        return Result.success(isOnline);
    }
}
