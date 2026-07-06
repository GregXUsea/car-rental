package com.carrental.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.carrental.dto.Result;
import com.carrental.entity.*;
import com.carrental.mapper.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 管理员控制器 - 管理后台专用接口
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private CarMapper carMapper;

    @Autowired
    private DriverMapper driverMapper;

    // 检查是否管理员
    private boolean isAdmin(HttpServletRequest request) {
        Integer role = (Integer) request.getAttribute("role");
        return role != null && role == 1;
    }

    /**
     * 仪表盘数据
     */
    @GetMapping("/dashboard")
    public Result<Map<String, Object>> getDashboard(HttpServletRequest request) {
        if (!isAdmin(request)) return Result.error("无权限");

        Map<String, Object> data = new HashMap<>();

        // 今日订单数
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime todayEnd = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        long todayOrders = orderMapper.selectCount(
                new LambdaQueryWrapper<Order>()
                        .between(Order::getCreateTime, todayStart, todayEnd)
        );
        data.put("todayOrders", todayOrders);

        // 在租车辆数
        long activeRentals = orderMapper.selectCount(
                new LambdaQueryWrapper<Order>().eq(Order::getStatus, 1)
        );
        data.put("activeRentals", activeRentals);

        // 待处理订单（待支付 + 预约中）
        long pendingOrders = orderMapper.selectCount(
                new LambdaQueryWrapper<Order>()
                        .in(Order::getStatus, 0, 4)
        );
        data.put("pendingOrders", pendingOrders);

        // 总收入（已完成订单）
        List<Order> completedOrders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>().eq(Order::getStatus, 2)
        );
        BigDecimal totalRevenue = completedOrders.stream()
                .map(o -> o.getTotalCost() != null ? o.getTotalCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        data.put("totalRevenue", totalRevenue);

        // 总用户数
        long totalUsers = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getRole, 0)
        );
        data.put("totalUsers", totalUsers);

        // 总车辆数
        long totalCars = carMapper.selectCount(null);
        data.put("totalCars", totalCars);

        // 可用车辆数
        long availableCars = carMapper.selectCount(
                new LambdaQueryWrapper<Car>().eq(Car::getStatus, 0)
        );
        data.put("availableCars", availableCars);

        // 最近订单
        List<Order> recentOrders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .orderByDesc(Order::getCreateTime)
                        .last("LIMIT 10")
        );

        // 填充订单的用户名和车辆信息
        fillOrderDetails(recentOrders);
        data.put("recentOrders", recentOrders);

        return Result.success(data);
    }

    /**
     * 用户列表
     */
    @GetMapping("/users")
    public Result<List<Map<String, Object>>> getUsers(HttpServletRequest request) {
        if (!isAdmin(request)) return Result.error("无权限");

        List<User> users = userMapper.selectList(
                new LambdaQueryWrapper<User>()
                        .eq(User::getRole, 0)
                        .orderByDesc(User::getCreateTime)
        );

        List<Map<String, Object>> result = new ArrayList<>();
        for (User user : users) {
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", user.getId());
            userMap.put("username", user.getUsername());
            userMap.put("nickname", user.getNickname());
            userMap.put("phone", user.getPhone());
            userMap.put("email", user.getEmail());
            userMap.put("avatar", user.getAvatar());
            userMap.put("createTime", user.getCreateTime());

            // 统计用户订单数
            long orderCount = orderMapper.selectCount(
                    new LambdaQueryWrapper<Order>().eq(Order::getUserId, user.getId())
            );
            userMap.put("orderCount", orderCount);

            result.add(userMap);
        }

        return Result.success(result);
    }

    /**
     * 用户详情（含订单和优惠券）
     */
    @GetMapping("/users/{userId}")
    public Result<Map<String, Object>> getUserDetail(@PathVariable Long userId, HttpServletRequest request) {
        if (!isAdmin(request)) return Result.error("无权限");

        User user = userMapper.selectById(userId);
        if (user == null) return Result.error("用户不存在");

        Map<String, Object> result = new HashMap<>();
        result.put("id", user.getId());
        result.put("username", user.getUsername());
        result.put("nickname", user.getNickname());
        result.put("phone", user.getPhone());
        result.put("email", user.getEmail());
        result.put("createTime", user.getCreateTime());

        // 用户订单
        List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getUserId, userId)
                        .orderByDesc(Order::getCreateTime)
        );
        fillOrderDetails(orders);
        result.put("orders", orders);

        // 用户优惠券（暂不查询， Coupon 实体不存在）
        result.put("coupons", new ArrayList<>());

        return Result.success(result);
    }

    /**
     * 所有订单（带筛选）
     */
    @GetMapping("/orders")
    public Result<List<Order>> getOrders(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long userId,
            HttpServletRequest request) {
        if (!isAdmin(request)) return Result.error("无权限");

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }
        if (userId != null) {
            wrapper.eq(Order::getUserId, userId);
        }
        wrapper.orderByDesc(Order::getCreateTime);

        List<Order> orders = orderMapper.selectList(wrapper);
        fillOrderDetails(orders);

        return Result.success(orders);
    }

    /**
     * 优惠券统计
     */
    @GetMapping("/coupons")
    public Result<Map<String, Object>> getCouponStats(HttpServletRequest request) {
        if (!isAdmin(request)) return Result.error("无权限");

        Map<String, Object> result = new HashMap<>();
        result.put("totalCoupons", 0);
        result.put("unusedCoupons", 0);
        result.put("usedCoupons", 0);
        result.put("expiredCoupons", 0);
        result.put("couponList", new ArrayList<>());

        return Result.success(result);
    }

    /**
     * 填充订单的用户名和车辆信息
     */
    private void fillOrderDetails(List<Order> orders) {
        if (orders == null || orders.isEmpty()) return;

        // 批量获取用户和车辆
        Set<Long> userIds = orders.stream().map(Order::getUserId).collect(Collectors.toSet());
        Set<Long> carIds = orders.stream().map(Order::getCarId).collect(Collectors.toSet());

        Map<Long, String> userNames = new HashMap<>();
        userMapper.selectBatchIds(userIds).forEach(u ->
                userNames.put(u.getId(), u.getNickname() != null ? u.getNickname() : u.getUsername())
        );

        Map<Long, Car> carMap = new HashMap<>();
        carMapper.selectBatchIds(carIds).forEach(c -> carMap.put(c.getId(), c));

        // 设置关联数据
        orders.forEach(order -> {
            order.setUsername(userNames.get(order.getUserId()));
            order.setCar(carMap.get(order.getCarId()));
        });
    }
}
