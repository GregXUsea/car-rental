package com.carrental.controller;

import com.carrental.dto.*;
import com.carrental.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public Result<String> login(@Valid @RequestBody LoginDTO dto) {
        return userService.login(dto);
    }

    @PostMapping("/register")
    public Result<String> register(@Valid @RequestBody RegisterDTO dto) {
        return userService.register(dto);
    }

    @GetMapping("/check-username")
    public Result<String> checkUsername(@RequestParam String username) {
        return userService.checkUsername(username);
    }

    @PostMapping("/reset-password")
    public Result<String> resetPassword(@RequestBody java.util.Map<String, String> body) {
        return userService.resetPassword(body.get("username"), body.get("phone"), body.get("newPassword"));
    }

    /**
     * 验证用户名+邮箱匹配用户
     */
    @PostMapping("/verify-identity")
    public Result<String> verifyIdentity(@RequestBody java.util.Map<String, String> body) {
        return userService.verifyIdentity(body.get("username"), body.get("email"));
    }

    /**
     * 校验验证码（不重置密码）
     */
    @PostMapping("/verify-code")
    public Result<String> verifyCode(@RequestBody java.util.Map<String, String> body) {
        return userService.verifyCode(body.get("username"), body.get("email"), body.get("code"));
    }

    /**
     * 发送邮箱验证码
     */
    @PostMapping("/send-reset-code")
    public Result<String> sendResetCode(@RequestBody java.util.Map<String, String> body) {
        return userService.sendResetCode(body.get("username"), body.get("email"));
    }

    /**
     * 验证码重置密码
     */
    @PostMapping("/reset-password-by-code")
    public Result<String> resetPasswordByCode(@RequestBody java.util.Map<String, String> body) {
        return userService.resetPasswordByCode(
                body.get("username"), body.get("email"),
                body.get("code"), body.get("newPassword"));
    }
}
