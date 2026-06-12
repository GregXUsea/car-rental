package com.carrental.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.carrental.dto.RentDTO;
import com.carrental.entity.Car;
import com.carrental.entity.Order;
import com.carrental.mapper.OrderMapper;
import com.carrental.util.OrderNoUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private CarService carService;

    public List<Order> listAll() {
        List<Order> orders = orderMapper.selectAllWithCarAndUser();
        return orders;
    }

    public List<Order> listByUserId(Long userId) {
        return orderMapper.selectByUserId(userId);
    }

    public Order getById(Long id) {
        return orderMapper.selectById(id);
    }

    @Transactional
    public Order createOrder(Long userId, RentDTO dto) {
        Car car = carService.getById(dto.getCarId());
        if (car == null) throw new RuntimeException("车辆不存在");

        boolean isReservation = Boolean.TRUE.equals(dto.getIsReservation());

        if (isReservation) {
            if (car.getStatus() != 0) {
                throw new RuntimeException("车辆当前不可预约");
            }
            Long existReserve = orderMapper.selectCount(
                    new LambdaQueryWrapper<Order>()
                            .eq(Order::getCarId, dto.getCarId())
                            .eq(Order::getStatus, 4));
            if (existReserve > 0) {
                throw new RuntimeException("该车已被预约，不能重复预约");
            }
        } else {
            if (car.getStatus() != 0) {
                throw new RuntimeException("车辆当前不可租用");
            }
        }

        // === 租车时间校验 ===
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = dto.getStartTime();
        LocalDateTime endTime = dto.getEndTime();

        // 计算最早可租时间：14:00之前可租当天，14:00之后最早明天8:00
        LocalDateTime earliestStart;
        if (now.getHour() < 14) {
            earliestStart = now.toLocalDate().atTime(8, 0);
        } else {
            earliestStart = now.toLocalDate().plusDays(1).atTime(8, 0);
        }

        if (startTime.isBefore(earliestStart)) {
            if (now.getHour() < 14) {
                throw new RuntimeException("开始时间不能早于今天 08:00");
            } else {
                throw new RuntimeException("当前已过14:00，最早只能从明天 08:00 开始租");
            }
        }

        if (!endTime.isAfter(startTime)) {
            throw new RuntimeException("结束时间必须晚于开始时间");
        }

        long days = Duration.between(startTime, endTime).toDays();
        if (days < 1) days = 1;
        if (days > 15) {
            throw new RuntimeException("最多只能租15天");
        }

        Order order = new Order();
        order.setOrderNo(OrderNoUtil.generate());
        order.setUserId(userId);
        order.setCarId(dto.getCarId());
        order.setStartTime(startTime);
        order.setEndTime(endTime);
        order.setDeposit(car.getDeposit());
        order.setTotalCost(car.getPricePerDay().multiply(BigDecimal.valueOf(days)));
        order.setStatus(isReservation ? 4 : 0);
        order.setCreateTime(LocalDateTime.now());

        orderMapper.insert(order);
        carService.updateStatus(dto.getCarId(), isReservation ? 2 : 1);

        return order;
    }

    @Transactional
    public Order payOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) throw new RuntimeException("订单不存在");
        if (order.getStatus() != 0 && order.getStatus() != 4) {
            throw new RuntimeException("订单状态不允许支付");
        }
        order.setStatus(1);
        orderMapper.updateById(order);
        carService.updateStatus(order.getCarId(), 1);
        return order;
    }

    @Transactional
    public Order returnCar(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) throw new RuntimeException("订单不存在");
        if (order.getStatus() != 1) {
            throw new RuntimeException("订单状态不允许归还");
        }

        order.setActualReturnTime(LocalDateTime.now());

        // 计算实际费用
        long days = Duration.between(order.getStartTime(), order.getActualReturnTime()).toDays();
        if (days < 1) days = 1;
        Car car = carService.getById(order.getCarId());
        order.setTotalCost(car.getPricePerDay().multiply(BigDecimal.valueOf(days)));
        order.setStatus(2);

        orderMapper.updateById(order);
        carService.updateStatus(order.getCarId(), 0);

        return order;
    }

    @Transactional
    public Order cancelOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) throw new RuntimeException("订单不存在");
        if (order.getStatus() != 0 && order.getStatus() != 4) {
            throw new RuntimeException("订单状态不允许取消");
        }
        order.setStatus(3);
        orderMapper.updateById(order);
        carService.updateStatus(order.getCarId(), 0);
        return order;
    }
}
