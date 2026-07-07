package com.carrental.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.carrental.dto.*;
import com.carrental.entity.User;
import com.carrental.entity.VerificationCode;
import com.carrental.mapper.UserMapper;
import com.carrental.mapper.VerificationCodeMapper;
import com.carrental.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

import java.time.LocalDateTime;

@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtil jwtUtil;

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

    /**
     * 管理员登录发送验证码
     */
    public Result<String> adminSendCode(String username, String email) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) return Result.error("用户不存在");
        if (user.getRole() != 1) return Result.error("非管理员账号");
        if (user.getEmail() == null || !user.getEmail().equalsIgnoreCase(email)) {
            return Result.error("邮箱不匹配");
        }

        // 生成6位验证码
        String code = String.valueOf((int)(Math.random() * 900000 + 100000));

        // 保存验证码
        VerificationCode vc = new VerificationCode();
        vc.setUserId(user.getId());
        vc.setEmail(email);
        vc.setCode(code);
        vc.setExpireTime(LocalDateTime.now().plusMinutes(5));
        verificationCodeMapper.insert(vc);

        // 发送邮件
        emailService.sendVerificationCode(email, code);

        return Result.success("验证码已发送");
    }

    /**
     * 管理员登录验证
     */
    public Result<String> adminLogin(String username, String password, String code) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) return Result.error("用户不存在");
        if (user.getRole() != 1) return Result.error("非管理员账号");
        if (!checkPassword(password, user.getPassword())) {
            return Result.error("密码错误");
        }

        // 验证验证码
        VerificationCode vc = verificationCodeMapper.selectOne(
                new LambdaQueryWrapper<VerificationCode>()
                        .eq(VerificationCode::getUserId, user.getId())
                        .eq(VerificationCode::getCode, code)
                        .ge(VerificationCode::getExpireTime, LocalDateTime.now())
                        .orderByDesc(VerificationCode::getCreateTime)
                        .last("LIMIT 1"));

        if (vc == null) {
            return Result.error("验证码错误或已过期");
        }

        // 标记验证码已使用
        verificationCodeMapper.deleteById(vc.getId());

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        return Result.success(token);
    }

    public Result<String> checkUsername(String username) {
        if (username == null || username.length() < 2) {
            return Result.error("用户名至少需要2个字符");
        }
        User exist = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (exist != null) {
            return Result.error("该用户名已被使用");
        }
        return Result.success("用户名可用");
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

    public User getUserByUsername(String username) {
        return userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }

    public void updateUserCreateTime(User user) {
        userMapper.updateById(user);
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
        if (checkPassword(dto.getNewPassword(), user.getPassword())) {
            return Result.error("新密码不能与当前密码相同");
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

    public Result<String> resetPassword(String username, String phone, String newPassword) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) return Result.error("用户不存在");
        if (user.getPhone() == null || !user.getPhone().equals(phone)) {
            return Result.error("手机号验证失败");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
        return Result.success("密码重置成功");
    }

    // ====== 邮箱验证码找回密码 ======

    @Autowired
    private VerificationCodeMapper verificationCodeMapper;

    @Autowired
    private EmailService emailService;

    /**
     * 验证手机号+邮箱匹配用户
     */
    public Result<String> verifyIdentity(String username, String email) {
        if (username == null || username.isBlank()) return Result.error("请输入用户名");
        if (email == null || email.isBlank()) return Result.error("请输入邮箱");

        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) return Result.error("该用户名不存在");
        if (user.getEmail() == null || !user.getEmail().equalsIgnoreCase(email)) {
            return Result.error("用户名与邮箱不匹配");
        }
        return Result.success(String.valueOf(user.getId()));
    }

    /**
     * 发送验证码到邮箱
     */
    public Result<String> sendResetCode(String username, String email) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) return Result.error("用户不存在");
        if (user.getEmail() == null || !user.getEmail().equalsIgnoreCase(email)) {
            return Result.error("用户名与邮箱不匹配");
        }

        // 防刷：1分钟内只能发一次
        LocalDateTime oneMinuteAgo = LocalDateTime.now().minusMinutes(1);
        Long recentCount = verificationCodeMapper.selectCount(
                new LambdaQueryWrapper<VerificationCode>()
                        .eq(VerificationCode::getUserId, user.getId())
                        .ge(VerificationCode::getCreateTime, oneMinuteAgo));
        if (recentCount > 0) {
            return Result.error("请等待1分钟后再发送");
        }

        // 防刷：1小时内最多5次
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        Long hourCount = verificationCodeMapper.selectCount(
                new LambdaQueryWrapper<VerificationCode>()
                        .eq(VerificationCode::getUserId, user.getId())
                        .ge(VerificationCode::getCreateTime, oneHourAgo));
        if (hourCount >= 5) {
            return Result.error("发送次数过多，请1小时后再试");
        }

        // 生成6位验证码
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));

        // 存入数据库
        VerificationCode vc = new VerificationCode();
        vc.setUserId(user.getId());
        vc.setEmail(email);
        vc.setCode(code);
        vc.setUsed(0);
        vc.setCreateTime(LocalDateTime.now());
        vc.setExpireTime(LocalDateTime.now().plusMinutes(5));
        verificationCodeMapper.insert(vc);

        // 发送邮件
        String result = emailService.sendVerificationCode(email, code);
        if (result == null) {
            return Result.error("邮件发送失败，请稍后重试");
        }
        // 开发模式：SMTP未配置时，result就是验证码
        if (!result.equals("sent")) {
            return Result.success("验证码已发送（开发模式: " + result + "）");
        }

        return Result.success("验证码已发送至您的邮箱");
    }

    /**
     * 仅校验验证码（不重置密码）
     */
    public Result<String> verifyCode(String username, String email, String code) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) return Result.error("用户不存在");
        if (user.getEmail() == null || !user.getEmail().equalsIgnoreCase(email)) {
            return Result.error("用户名与邮箱不匹配");
        }

        VerificationCode vc = verificationCodeMapper.selectOne(
                new LambdaQueryWrapper<VerificationCode>()
                        .eq(VerificationCode::getUserId, user.getId())
                        .eq(VerificationCode::getCode, code)
                        .eq(VerificationCode::getUsed, 0)
                        .orderByDesc(VerificationCode::getCreateTime)
                        .last("LIMIT 1"));

        if (vc == null) return Result.error("验证码错误");
        if (vc.getExpireTime().isBefore(LocalDateTime.now())) return Result.error("验证码已过期，请重新获取");

        return Result.success("验证码正确");
    }

    /**
     * 验证码校验 + 重置密码
     */
    public Result<String> resetPasswordByCode(String username, String email, String code, String newPassword) {
        // 校验用户
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) return Result.error("用户不存在");
        if (user.getEmail() == null || !user.getEmail().equalsIgnoreCase(email)) {
            return Result.error("用户名与邮箱不匹配");
        }

        // 校验新密码
        if (newPassword == null || newPassword.length() < 6) {
            return Result.error("新密码长度不能少于6位");
        }
        if (checkPassword(newPassword, user.getPassword())) {
            return Result.error("新密码不能与当前密码相同");
        }

        // 查找有效验证码
        VerificationCode vc = verificationCodeMapper.selectOne(
                new LambdaQueryWrapper<VerificationCode>()
                        .eq(VerificationCode::getUserId, user.getId())
                        .eq(VerificationCode::getCode, code)
                        .eq(VerificationCode::getUsed, 0)
                        .orderByDesc(VerificationCode::getCreateTime)
                        .last("LIMIT 1"));

        if (vc == null) {
            return Result.error("验证码错误");
        }
        if (vc.getExpireTime().isBefore(LocalDateTime.now())) {
            return Result.error("验证码已过期，请重新获取");
        }

        // 标记已使用
        vc.setUsed(1);
        verificationCodeMapper.updateById(vc);

        // 重置密码
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);

        // 清理该用户所有未使用的验证码
        verificationCodeMapper.delete(
                new LambdaQueryWrapper<VerificationCode>()
                        .eq(VerificationCode::getUserId, user.getId())
                        .eq(VerificationCode::getUsed, 0));

        return Result.success("密码重置成功");
    }
}
