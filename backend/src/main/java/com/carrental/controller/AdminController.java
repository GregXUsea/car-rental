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

        // 今日新增用户
        long todayNewUsers = userMapper.selectCount(
                new LambdaQueryWrapper<User>()
                        .eq(User::getRole, 0)
                        .between(User::getCreateTime, todayStart, todayEnd)
        );
        data.put("todayNewUsers", todayNewUsers);

        // 在租订单详情
        List<Order> activeOrders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getStatus, 1)
                        .orderByDesc(Order::getStartTime)
                        .last("LIMIT 5")
        );
        fillOrderDetails(activeOrders);
        data.put("activeOrders", activeOrders);

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
     * 订单评价汇总
     */
    @GetMapping("/reviews")
    public Result<Map<String, Object>> getReviews(HttpServletRequest request) {
        if (!isAdmin(request)) return Result.error("无权限");

        Map<String, Object> result = new HashMap<>();

        // 获取所有已评价的订单
        List<Order> reviewedOrders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getStatus, 2)
                        .isNotNull(Order::getUserRating)
                        .orderByDesc(Order::getCreateTime)
        );
        fillOrderDetails(reviewedOrders);

        // 统计评分分布
        long star5 = reviewedOrders.stream().filter(o -> o.getUserRating() != null && o.getUserRating() == 5).count();
        long star4 = reviewedOrders.stream().filter(o -> o.getUserRating() != null && o.getUserRating() == 4).count();
        long star3 = reviewedOrders.stream().filter(o -> o.getUserRating() != null && o.getUserRating() == 3).count();
        long star2 = reviewedOrders.stream().filter(o -> o.getUserRating() != null && o.getUserRating() == 2).count();
        long star1 = reviewedOrders.stream().filter(o -> o.getUserRating() != null && o.getUserRating() == 1).count();

        double avgRating = reviewedOrders.stream()
                .mapToInt(o -> o.getUserRating() != null ? o.getUserRating() : 0)
                .average()
                .orElse(0);

        result.put("totalReviews", reviewedOrders.size());
        result.put("avgRating", Math.round(avgRating * 10) / 10.0);
        result.put("star5", star5);
        result.put("star4", star4);
        result.put("star3", star3);
        result.put("star2", star2);
        result.put("star1", star1);
        result.put("reviews", reviewedOrders);

        return Result.success(result);
    }

    /**
     * 优惠券统计
     */
    @GetMapping("/coupons")
    public Result<Map<String, Object>> getCouponStats(HttpServletRequest request) {
        if (!isAdmin(request)) return Result.error("无权限");

        Map<String, Object> result = new HashMap<>();

        // 获取所有优惠券
        List<Coupon> coupons = couponMapper.selectList(
                new LambdaQueryWrapper<Coupon>().orderByDesc(Coupon::getCreateTime)
        );

        // 统计
        long totalCoupons = coupons.size();
        long unusedCoupons = coupons.stream().filter(c -> c.getStatus() == 0).count();
        long usedCoupons = coupons.stream().filter(c -> c.getStatus() == 1).count();
        long expiredCoupons = coupons.stream().filter(c -> c.getStatus() == 2).count();

        result.put("totalCoupons", totalCoupons);
        result.put("unusedCoupons", unusedCoupons);
        result.put("usedCoupons", usedCoupons);
        result.put("expiredCoupons", expiredCoupons);
        result.put("couponList", coupons);

        return Result.success(result);
    }

    /**
     * 发放优惠券给用户
     */
    @PostMapping("/coupons/give")
    public Result<Coupon> giveCoupon(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        if (!isAdmin(request)) return Result.error("无权限");

        Long userId = Long.valueOf(body.get("userId").toString());
        Integer type = Integer.valueOf(body.get("type").toString());
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        BigDecimal rate = body.get("rate") != null ? new BigDecimal(body.get("rate").toString()) : BigDecimal.ONE;
        BigDecimal minAmount = body.get("minAmount") != null ? new BigDecimal(body.get("minAmount").toString()) : BigDecimal.ZERO;

        Coupon coupon = new Coupon();
        coupon.setUserId(userId);
        coupon.setCouponCode(generateCouponCode());
        coupon.setCouponType(type);
        coupon.setDiscountAmount(type == 1 ? amount : BigDecimal.ZERO);
        coupon.setDiscountRate(type == 2 ? rate : BigDecimal.ONE);
        coupon.setMinAmount(minAmount);
        coupon.setStatus(0);
        coupon.setExpireTime(LocalDateTime.now().plusDays(30));
        couponMapper.insert(coupon);

        return Result.success(coupon);
    }

    private String generateCouponCode() {
        return "CP" + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }

    /**
     * 获取所有车辆（管理员用）
     */
    @GetMapping("/cars")
    public Result<List<Car>> getAllCars(HttpServletRequest request) {
        if (!isAdmin(request)) return Result.error("无权限");
        List<Car> cars = carMapper.selectList(
                new LambdaQueryWrapper<Car>().orderByDesc(Car::getCreateTime)
        );
        return Result.success(cars);
    }

    /**
     * 添加车辆（管理员用）
     */
    @PostMapping("/cars")
    public Result<Car> addCar(@RequestBody Car car, HttpServletRequest request) {
        if (!isAdmin(request)) return Result.error("无权限");
        car.setCreateTime(LocalDateTime.now());
        carMapper.insert(car);
        return Result.success(car);
    }

    /**
     * 更新车辆（管理员用）
     */
    @PutMapping("/cars/{id}")
    public Result<Car> updateCar(@PathVariable Long id, @RequestBody Car car, HttpServletRequest request) {
        if (!isAdmin(request)) return Result.error("无权限");
        car.setId(id);
        carMapper.updateById(car);
        return Result.success(car);
    }

    /**
     * 删除车辆（管理员用）
     */
    @DeleteMapping("/cars/{id}")
    public Result<Void> deleteCar(@PathVariable Long id, HttpServletRequest request) {
        if (!isAdmin(request)) return Result.error("无权限");
        carMapper.deleteById(id);
        return Result.success(null);
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
