package com.carrental.controller;

import com.carrental.dto.*;
import com.carrental.entity.User;
import com.carrental.service.OrderService;
import com.carrental.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private OrderService orderService;

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

    @GetMapping("/coupon-status")
    public Result<Map<String, Object>> getCouponStatus(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        User user = userService.getUserById(userId);
        if (user == null) return Result.error("用户不存在");

        Map<String, Object> result = new HashMap<>();
        boolean eligible = orderService.checkNewUserCoupon(userId);
        result.put("eligible", eligible);
        result.put("registerDate", user.getCreateTime() != null ? user.getCreateTime().toString() : null);

        if (user.getCreateTime() != null) {
            long daysLeft = 30 - ChronoUnit.DAYS.between(user.getCreateTime(), LocalDateTime.now());
            result.put("daysLeft", Math.max(0, daysLeft));
            result.put("expireDate", user.getCreateTime().plusDays(30).toString());
        } else {
            result.put("daysLeft", 0);
            result.put("expireDate", null);
        }

        return Result.success(result);
    }
}
