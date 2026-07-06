package com.carrental.controller;

import com.carrental.dto.RentDTO;
import com.carrental.dto.Result;
import com.carrental.entity.Order;
import com.carrental.entity.User;
import com.carrental.service.OrderService;
import com.carrental.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserService userService;

    // 检查是否管理员
    private boolean isAdmin(HttpServletRequest request) {
        Integer role = (Integer) request.getAttribute("role");
        return role != null && role == 1;
    }

    @GetMapping("/my")
    public Result<List<Order>> myOrders(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(orderService.listByUserId(userId));
    }

    @GetMapping("/all")
    public Result<List<Order>> allOrders(HttpServletRequest request) {
        if (!isAdmin(request)) return Result.error("无权限，仅管理员可操作");
        return Result.success(orderService.listAll());
    }

    @GetMapping("/detail/{id}")
    public Result<Order> detail(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Order order = orderService.getById(id);
        if (order == null) return Result.error("订单不存在");
        // 非管理员只能看自己的订单
        if (!isAdmin(request) && !order.getUserId().equals(userId)) {
            return Result.error("无权查看此订单");
        }
        return Result.success(order);
    }

    @PostMapping("/rent")
    public Result<Order> rent(HttpServletRequest request, @Valid @RequestBody RentDTO dto) {
        Long userId = (Long) request.getAttribute("userId");
        try {
            Order order = orderService.createOrder(userId, dto);
            return Result.success(order);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/pay/{id}")
    public Result<Order> pay(@PathVariable Long id) {
        try {
            return Result.success(orderService.payOrder(id));
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/pay-deposit/{id}")
    public Result<Order> payDeposit(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        try {
            return Result.success(orderService.payDeposit(id, userId));
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/pay-rental/{id}")
    public Result<Order> payRental(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        try {
            return Result.success(orderService.payRental(id, userId));
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/slots/{carId}")
    public Result<List<Map<String, Object>>> getOccupiedSlots(@PathVariable Long carId) {
        return Result.success(orderService.getOccupiedSlots(carId));
    }

    @PostMapping("/return/{id}")
    public Result<Order> returnCar(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        try {
            return Result.success(orderService.returnCar(id, userId));
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/confirm-pickup/{id}")
    public Result<Order> confirmPickup(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        try {
            return Result.success(orderService.confirmPickup(id, userId));
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/early-return/{id}")
    public Result<Order> earlyReturn(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        try {
            return Result.success(orderService.returnCar(id, userId, true));
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/cancel/{id}")
    public Result<Order> cancel(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            return Result.success(orderService.cancelOrder(id, userId));
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/cancel-info/{id}")
    public Result<Map<String, Object>> cancelInfo(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(orderService.getCancelInfo(id, userId));
    }

    @PostMapping("/rate/{id}")
    public Result<Order> rate(@PathVariable Long id, @RequestBody java.util.Map<String, Object> body, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            Integer rating = body.get("rating") != null ? ((Number) body.get("rating")).intValue() : null;
            String comment = (String) body.get("comment");
            if (rating == null) return Result.error("请提供评分");
            return Result.success(orderService.rateOrder(id, userId, rating, comment));
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    // 管理员：重置指定用户的优惠券（取消所有非取消订单）
    @PostMapping("/admin/reset-coupon")
    public Result<Map<String, Object>> resetCoupon(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        if (!isAdmin(request)) return Result.error("无权限，仅管理员可操作");
        try {
            @SuppressWarnings("unchecked")
            List<String> usernames = (List<String>) body.get("usernames");
            if (usernames == null || usernames.isEmpty()) {
                return Result.error("请提供用户名列表");
            }
            Map<String, Object> result = new HashMap<>();
            int totalReset = 0;
            for (String username : usernames) {
                User user = userService.getUserByUsername(username);
                if (user != null) {
                    int count = orderService.resetCouponWithRefresh(user.getId(), user);
                    result.put(username, Map.of("userId", user.getId(), "cancelledOrders", count));
                    totalReset += count;
                } else {
                    result.put(username, Map.of("error", "用户不存在"));
                }
            }
            result.put("totalReset", totalReset);
            return Result.success(result);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    // 管理员：恢复被误取消的订单
    @PostMapping("/admin/restore-orders")
    public Result<Map<String, Object>> restoreOrders(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        if (!isAdmin(request)) return Result.error("无权限，仅管理员可操作");
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> orders = (List<Map<String, Object>>) body.get("orders");
            if (orders == null || orders.isEmpty()) {
                return Result.error("请提供订单列表");
            }
            Map<String, Object> result = new HashMap<>();
            for (Map<String, Object> item : orders) {
                Long orderId = Long.valueOf(item.get("orderId").toString());
                Integer status = Integer.valueOf(item.get("status").toString());
                orderService.restoreOrder(orderId, status);
                result.put("order_" + orderId, "restored to status " + status);
            }
            return Result.success(result);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    // 管理员：刷新指定订单的取消窗口
    @PostMapping("/admin/refresh-cancel-window")
    public Result<Map<String, Object>> refreshCancelWindow(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        if (!isAdmin(request)) return Result.error("无权限，仅管理员可操作");
        try {
            @SuppressWarnings("unchecked")
            List<Number> rawList = (List<Number>) body.get("orderIds");
            List<Long> orderIds = rawList.stream()
                    .map(Number::longValue).toList();
            Map<String, Object> result = new HashMap<>();
            for (Long orderId : orderIds) {
                orderService.refreshCancelWindow(orderId);
                result.put("order_" + orderId, "refreshed");
            }
            return Result.success(result);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
}
