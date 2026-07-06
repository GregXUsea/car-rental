package com.carrental.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.carrental.entity.Message;
import com.carrental.entity.User;
import com.carrental.mapper.MessageMapper;
import com.carrental.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 消息服务 - 管理员与用户沟通
 */
@Service
public class MessageService {

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * 发送消息
     */
    public Message sendMessage(Long senderId, Long receiverId, String content) {
        Message msg = new Message();
        msg.setSenderId(senderId);
        msg.setReceiverId(receiverId);
        msg.setContent(content);
        msg.setIsRead(0);
        messageMapper.insert(msg);
        return msg;
    }

    /**
     * 获取两个用户之间的对话
     */
    public List<Message> getConversation(Long userId1, Long userId2) {
        List<Message> messages = messageMapper.selectList(
                new LambdaQueryWrapper<Message>()
                        .and(q -> q
                                .and(q1 -> q1.eq(Message::getSenderId, userId1).eq(Message::getReceiverId, userId2))
                                .or(q2 -> q2.eq(Message::getSenderId, userId2).eq(Message::getReceiverId, userId1))
                        )
                        .orderByAsc(Message::getCreateTime)
        );

        // 填充用户名
        Set<Long> userIds = new HashSet<>();
        messages.forEach(m -> {
            userIds.add(m.getSenderId());
            userIds.add(m.getReceiverId());
        });

        Map<Long, String> userNames = new HashMap<>();
        userMapper.selectBatchIds(userIds).forEach(u -> userNames.put(u.getId(), u.getNickname() != null ? u.getNickname() : u.getUsername()));

        messages.forEach(m -> {
            m.setSenderName(userNames.get(m.getSenderId()));
            m.setReceiverName(userNames.get(m.getReceiverId()));
        });

        return messages;
    }

    /**
     * 获取所有对话列表（管理员用，按最新消息分组）
     */
    public List<Map<String, Object>> getConversationList(Long userId) {
        // 获取所有与该用户相关的消息
        List<Message> messages = messageMapper.selectList(
                new LambdaQueryWrapper<Message>()
                        .and(q -> q
                                .eq(Message::getSenderId, userId)
                                .or(q2 -> q2.eq(Message::getReceiverId, userId))
                        )
                        .orderByDesc(Message::getCreateTime)
        );

        // 按对方用户分组，取最新一条
        Map<Long, Message> latestMessages = new LinkedHashMap<>();
        for (Message msg : messages) {
            Long otherUserId = msg.getSenderId().equals(userId) ? msg.getReceiverId() : msg.getSenderId();
            if (!latestMessages.containsKey(otherUserId)) {
                latestMessages.put(otherUserId, msg);
            }
        }

        // 构建对话列表
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, Message> entry : latestMessages.entrySet()) {
            Long otherUserId = entry.getKey();
            Message latestMsg = entry.getValue();

            User otherUser = userMapper.selectById(otherUserId);
            if (otherUser == null) continue;

            // 计算未读消息数
            long unreadCount = messageMapper.selectCount(
                    new LambdaQueryWrapper<Message>()
                            .eq(Message::getSenderId, otherUserId)
                            .eq(Message::getReceiverId, userId)
                            .eq(Message::getIsRead, 0)
            );

            Map<String, Object> conversation = new HashMap<>();
            conversation.put("userId", otherUserId);
            conversation.put("userName", otherUser.getNickname() != null ? otherUser.getNickname() : otherUser.getUsername());
            conversation.put("userAvatar", otherUser.getAvatar());
            conversation.put("userRole", otherUser.getRole());
            conversation.put("latestMessage", latestMsg.getContent());
            conversation.put("latestTime", latestMsg.getCreateTime());
            conversation.put("unreadCount", unreadCount);
            result.add(conversation);
        }

        return result;
    }

    /**
     * 获取未读消息总数
     */
    public long getUnreadCount(Long userId) {
        return messageMapper.selectCount(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getReceiverId, userId)
                        .eq(Message::getIsRead, 0)
        );
    }

    /**
     * 标记消息已读
     */
    public void markAsRead(Long messageId) {
        Message msg = messageMapper.selectById(messageId);
        if (msg != null) {
            msg.setIsRead(1);
            messageMapper.updateById(msg);
        }
    }

    /**
     * 标记与某用户的所有消息为已读
     */
    public void markAllAsRead(Long senderId, Long receiverId) {
        List<Message> messages = messageMapper.selectList(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getSenderId, senderId)
                        .eq(Message::getReceiverId, receiverId)
                        .eq(Message::getIsRead, 0)
        );
        messages.forEach(msg -> {
            msg.setIsRead(1);
            messageMapper.updateById(msg);
        });
    }
}
