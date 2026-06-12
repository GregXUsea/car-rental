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

    /**
     * 发送找回密码验证码：输入用户名+邮箱，系统生成6位验证码
     */
    @PostMapping("/send-reset-code")
    public Result<String> sendResetCode(@Valid @RequestBody SendCodeDTO dto) {
        return userService.sendResetCode(dto);
    }

    /**
     * 找回密码：输入用户名+邮箱+验证码+新密码，验证通过后重置密码
     */
    @PostMapping("/reset-password")
    public Result<String> resetPassword(@Valid @RequestBody ResetPasswordDTO dto) {
        return userService.resetPassword(dto);
    }
}
