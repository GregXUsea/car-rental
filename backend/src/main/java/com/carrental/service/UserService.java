package com.carrental.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.carrental.dto.*;
import com.carrental.entity.User;
import com.carrental.mapper.UserMapper;
import com.carrental.util.JwtUtil;
import com.carrental.util.VerificationCodeStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private VerificationCodeStore codeStore;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public Result<String> login(LoginDTO dto) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (user == null) {
            return Result.error("用户不存在");
        }
        if (!checkPassword(dto.getPassword(), user.getPassword())) {
            return Result.error("密码错误");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        return Result.success(token);
    }

    public Result<String> register(RegisterDTO dto) {
        User exist = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (exist != null) {
            return Result.error("用户名已存在");
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname() != null ? dto.getNickname() : dto.getUsername());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setRole(0);
        user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);
        return Result.success("注册成功");
    }

    public User getUserById(Long id) {
        return userMapper.selectById(id);
    }

    public Result<String> updateUser(Long userId, UpdateUserDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) return Result.error("用户不存在");
        if (dto.getNickname() != null) user.setNickname(dto.getNickname());
        if (dto.getPhone() != null) user.setPhone(dto.getPhone());
        if (dto.getEmail() != null) user.setEmail(dto.getEmail());
        if (dto.getAvatar() != null) user.setAvatar(dto.getAvatar());
        userMapper.updateById(user);
        return Result.success("修改成功");
    }

    public Result<String> changePassword(Long userId, ChangePasswordDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) return Result.error("用户不存在");
        if (!checkPassword(dto.getOldPassword(), user.getPassword())) {
            return Result.error("旧密码错误");
        }
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userMapper.updateById(user);
        return Result.success("密码修改成功");
    }

    /**
     * 支持明文和BCrypt两种密码验证
     */
    private boolean checkPassword(String rawPassword, String storedPassword) {
        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$")) {
            return passwordEncoder.matches(rawPassword, storedPassword);
        }
        return rawPassword.equals(storedPassword);
    }

    /**
     * 发送找回密码验证码（演示版：验证码打印在控制台，生产环境应通过邮件发送）
     */
    public Result<String> sendResetCode(SendCodeDTO dto) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (user == null) return Result.error("用户不存在");
        if (user.getEmail() == null || !user.getEmail().equalsIgnoreCase(dto.getEmail())) {
            return Result.error("邮箱与注册邮箱不一致");
        }
        if (!codeStore.canSend(dto.getEmail())) {
            return Result.error("验证码已发送，请60秒后再试");
        }
        String code = codeStore.generateAndStore(dto.getEmail());
        if (code == null) {
            return Result.error("请稍后再试");
        }
        // 演示版：验证码直接返回，生产环境应通过邮件发送，不可返回给前端
        return Result.success(code);
    }

    /**
     * 找回密码：验证邮箱验证码后重置密码
     */
    public Result<String> resetPassword(ResetPasswordDTO dto) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (user == null) return Result.error("用户不存在");
        if (user.getEmail() == null || !user.getEmail().equalsIgnoreCase(dto.getEmail())) {
            return Result.error("邮箱与注册邮箱不一致");
        }
        if (!codeStore.verify(dto.getEmail(), dto.getCode())) {
            return Result.error("验证码错误或已过期");
        }
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userMapper.updateById(user);
        return Result.success("密码重置成功");
    }
}
