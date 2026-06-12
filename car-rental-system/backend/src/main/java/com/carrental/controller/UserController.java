package com.carrental.controller;

import com.carrental.dto.*;
import com.carrental.entity.User;
import com.carrental.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/info")
    public Result<User> getUserInfo(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        User user = userService.getUserById(userId);
        if (user != null) user.setPassword(null);
        return Result.success(user);
    }

    @PutMapping("/update")
    public Result<String> updateUser(HttpServletRequest request, @RequestBody UpdateUserDTO dto) {
        Long userId = (Long) request.getAttribute("userId");
        return userService.updateUser(userId, dto);
    }

    @PostMapping("/change-password")
    public Result<String> changePassword(HttpServletRequest request, @Valid @RequestBody ChangePasswordDTO dto) {
        Long userId = (Long) request.getAttribute("userId");
        return userService.changePassword(userId, dto);
    }
}
