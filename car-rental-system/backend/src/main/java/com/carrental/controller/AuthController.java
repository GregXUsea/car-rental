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

    @PostMapping("/reset-password")
    public Result<String> resetPassword(@RequestBody java.util.Map<String, String> body) {
        return userService.resetPassword(body.get("username"), body.get("phone"), body.get("newPassword"));
    }
}
