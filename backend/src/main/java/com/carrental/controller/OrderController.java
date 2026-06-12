package com.carrental.controller;

import com.carrental.dto.RentDTO;
import com.carrental.dto.Result;
import com.carrental.entity.Order;
import com.carrental.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @GetMapping("/my")
    public Result<List<Order>> myOrders(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(orderService.listByUserId(userId));
    }

    @GetMapping("/all")
    public Result<List<Order>> allOrders() {
        return Result.success(orderService.listAll());
    }

    @GetMapping("/detail/{id}")
    public Result<Order> detail(@PathVariable Long id) {
        Order order = orderService.getById(id);
        return order != null ? Result.success(order) : Result.error("订单不存在");
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

    @PostMapping("/return/{id}")
    public Result<Order> returnCar(@PathVariable Long id) {
        try {
            return Result.success(orderService.returnCar(id));
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/cancel/{id}")
    public Result<Order> cancel(@PathVariable Long id) {
        try {
            return Result.success(orderService.cancelOrder(id));
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
}
