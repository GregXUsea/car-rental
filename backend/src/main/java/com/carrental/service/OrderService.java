package com.carrental.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.carrental.dto.RentDTO;
import com.carrental.entity.Car;
import com.carrental.entity.Driver;
import com.carrental.entity.Order;
import com.carrental.entity.User;
import com.carrental.mapper.OrderMapper;
import com.carrental.util.OrderNoUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private CarService carService;

    @Autowired
    private DriverService driverService;

    @Autowired
    private RAGService ragService;

    @Autowired
    private UserService userService;

    // 司机服务费（每天）
    private static final BigDecimal DRIVER_DAILY_FEE = new BigDecimal("150.00");

    public List<Order> listAll() {
        List<Order> orders = orderMapper.selectAllWithCarAndUser();
        // 加载司机信息
        for (Order order : orders) {
            if (order.getDriverId() != null) {
                order.setDriver(driverService.getById(order.getDriverId()));
            }
        }
        return orders;
    }

    public List<Order> listByUserId(Long userId) {
        List<Order> orders = orderMapper.selectByUserId(userId);
        for (Order order : orders) {
            if (order.getDriverId() != null) {
                order.setDriver(driverService.getById(order.getDriverId()));
            }
        }
        return orders;
    }

    public Order getById(Long id) {
        Order order = orderMapper.selectById(id);
        if (order != null) {
            if (order.getCarId() != null) {
                order.setCar(carService.getById(order.getCarId()));
            }
            if (order.getDriverId() != null) {
                order.setDriver(driverService.getById(order.getDriverId()));
            }
        }
        return order;
    }

    @Transactional
    public Order createOrder(Long userId, RentDTO dto) {
        Car car = carService.getById(dto.getCarId());
        if (car == null) throw new RuntimeException("车辆不存在");

        boolean isReservation = Boolean.TRUE.equals(dto.getIsReservation());

        // 维护中的车不可操作
        if (car.getStatus() == 3) {
            throw new RuntimeException("车辆正在维护中，暂时不可使用");
        }

        if (isReservation) {
            // 预约：检查时间段是否与现有预约重叠
            List<Order> existingReserves = orderMapper.selectList(
                    new LambdaQueryWrapper<Order>()
                            .eq(Order::getCarId, dto.getCarId())
                            .eq(Order::getStatus, 4));
            for (Order reserve : existingReserves) {
                // 检查时间是否重叠（含6小时缓冲）
                LocalDateTime bufferedStart = reserve.getStartTime().minusHours(6);
                LocalDateTime bufferedEnd = reserve.getEndTime().plusHours(6);
                if (dto.getStartTime().isBefore(bufferedEnd) && dto.getEndTime().isAfter(bufferedStart)) {
                    throw new RuntimeException("该车在此时间段已有预约（含6小时周转缓冲）");
                }
            }
        }
        // 租车：不检查车辆状态，靠时间冲突检查来判断是否可用

        // === 租车时间校验 ===
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = dto.getStartTime();
        LocalDateTime endTime = dto.getEndTime();

        // 开始时间必须在当前时间之后
        if (!startTime.isAfter(now)) {
            throw new RuntimeException("开始时间必须晚于当前时间");
        }

        // 结束时间必须晚于开始时间
        if (!endTime.isAfter(startTime)) {
            throw new RuntimeException("结束时间必须晚于开始时间");
        }

        // 计算租期（精确到0.01天）
        long totalMinutes = Duration.between(startTime, endTime).toMinutes();
        BigDecimal days = BigDecimal.valueOf(totalMinutes).divide(BigDecimal.valueOf(1440), 2, java.math.RoundingMode.HALF_UP);
        if (days.compareTo(BigDecimal.ONE) < 0) days = BigDecimal.ONE;

        if (isReservation) {
            // 预约租车：最长2个月，最多20天
            LocalDateTime maxStart = now.plusMonths(2);
            if (startTime.isAfter(maxStart)) {
                throw new RuntimeException("预约最早只能提前2个月");
            }
            if (days.compareTo(BigDecimal.valueOf(20)) > 0) {
                throw new RuntimeException("预约租车最多20天");
            }
        } else {
            // 立即租车：必须在3天内开始，最多15天
            LocalDateTime latestStart = now.toLocalDate().plusDays(3).atTime(23, 59, 59);
            if (startTime.isAfter(latestStart)) {
                throw new RuntimeException("立即租车必须在3天内开始，超过3天请选择预约租车");
            }
            if (days.compareTo(BigDecimal.valueOf(15)) > 0) {
                throw new RuntimeException("立即租车最多15天");
            }
        }

        // 检查时间段是否与该车其他订单冲突
        if (hasTimeConflict(dto.getCarId(), startTime, endTime, null)) {
            throw new RuntimeException("该时间段车辆已被预约，请选择其他时间");
        }

        // 计算费用
        BigDecimal carCost = car.getPricePerDay().multiply(days);
        BigDecimal driverCost = BigDecimal.ZERO;
        Long driverId = null;

        if (dto.getDriverId() != null && dto.getDriverId() > 0) {
            Driver driver = driverService.getAvailableDriver(dto.getDriverId());
            if (driver == null) {
                throw new RuntimeException("所选司机不可用");
            }
            driverId = dto.getDriverId();
            driverCost = DRIVER_DAILY_FEE.multiply(days);
        }

        // 新用户优惠：首单5折（最高减200）
        BigDecimal discount = BigDecimal.ZERO;
        boolean isNewUser = isNewUser(userId);
        if (isNewUser) {
            BigDecimal originalTotal = carCost.add(driverCost);
            BigDecimal halfPriceDiscount = originalTotal.multiply(new BigDecimal("0.5"));
            discount = halfPriceDiscount.min(new BigDecimal("200"));
            carCost = carCost.subtract(discount);
        }

        Order order = new Order();
        order.setOrderNo(OrderNoUtil.generate());
        order.setUserId(userId);
        order.setCarId(dto.getCarId());
        order.setDriverId(driverId);
        order.setStartTime(startTime);
        order.setEndTime(endTime);
        order.setDiscount(discount);
        order.setDeposit(car.getDeposit());
        order.setTotalCost(carCost.add(driverCost));
        order.setDriverCost(driverCost);
        order.setStatus(isReservation ? 4 : 0);
        order.setRemark(dto.getRemark());
        order.setCreateTime(LocalDateTime.now());

        // 记录取车时里程
        if (dto.getStartMileage() != null) {
            order.setStartMileage(dto.getStartMileage());
        } else {
            order.setStartMileage(car.getMileage());
        }

        orderMapper.insert(order);

        // 车辆状态在支付押金后才更新，不在创建订单时更新
        // 预约的车：押金支付后才锁定
        // 租车：押金支付后才变为已租出

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

    /**
     * 支付押金
     */
    @Transactional
    public Order payDeposit(Long orderId, Long userId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) throw new RuntimeException("订单不存在");
        if (!order.getUserId().equals(userId)) throw new RuntimeException("无权操作此订单");
        if (order.getStatus() != 0 && order.getStatus() != 4) {
            throw new RuntimeException("订单状态不允许支付押金");
        }
        if (order.getDepositPaid() != null && order.getDepositPaid() == 1) {
            throw new RuntimeException("押金已支付，请勿重复操作");
        }
        order.setDepositPaid(1);
        order.setDepositPaidTime(LocalDateTime.now());

        if (order.getStatus() == 0) {
            // 租车：押金支付后变为在租
            order.setStatus(1);
            carService.updateStatus(order.getCarId(), 1);
        } else if (order.getStatus() == 4) {
            // 预约：押金支付后保持预约状态，但锁定车辆
            Car car = carService.getById(order.getCarId());
            if (car != null && car.getStatus() == 0) {
                carService.updateStatus(order.getCarId(), 2);
            }
        }

        // 更新司机状态
        if (order.getDriverId() != null) {
            driverService.updateStatus(order.getDriverId(), 1);
        }

        orderMapper.updateById(order);
        return order;
    }

    /**
     * 支付租金（可提前付，也可归还时付）
     */
    @Transactional
    public Order payRental(Long orderId, Long userId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) throw new RuntimeException("订单不存在");
        if (!order.getUserId().equals(userId)) throw new RuntimeException("无权操作此订单");
        if (order.getStatus() != 1 && order.getStatus() != 4) {
            throw new RuntimeException("订单状态不允许支付租金");
        }
        if (order.getRentalPaid() != null && order.getRentalPaid() == 1) {
            throw new RuntimeException("租金已支付，请勿重复操作");
        }
        order.setRentalPaid(1);
        order.setRentalPaidTime(LocalDateTime.now());
        orderMapper.updateById(order);
        return order;
    }

    /**
     * 获取车辆占用时间段（含6小时缓冲）
     */
    public List<Map<String, Object>> getOccupiedSlots(Long carId) {
        List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getCarId, carId)
                        .in(Order::getStatus, 1, 4) // 在租或预约中
                        .orderByAsc(Order::getStartTime));

        List<Map<String, Object>> slots = new ArrayList<>();
        for (Order order : orders) {
            Map<String, Object> slot = new HashMap<>();
            // 前后各扩展6小时
            LocalDateTime bufferedStart = order.getStartTime().minusHours(6);
            LocalDateTime bufferedEnd = order.getEndTime().plusHours(6);
            slot.put("start", order.getStartTime().toString());
            slot.put("end", order.getEndTime().toString());
            slot.put("bufferedStart", bufferedStart.toString());
            slot.put("bufferedEnd", bufferedEnd.toString());
            slot.put("type", order.getStatus() == 1 ? "rented" : "reserved");
            slot.put("orderId", order.getId());
            slots.add(slot);
        }
        return slots;
    }

    /**
     * 确认取车 - 用户确认已取到车辆，开始计时
     */
    @Transactional
    public Order confirmPickup(Long orderId, Long userId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) throw new RuntimeException("订单不存在");
        if (!order.getUserId().equals(userId)) throw new RuntimeException("无权操作此订单");
        if (order.getStatus() != 1) {
            throw new RuntimeException("订单状态不允许确认取车");
        }
        if (order.getDepositPaid() == null || order.getDepositPaid() != 1) {
            throw new RuntimeException("请先支付押金");
        }
        if (order.getPickupConfirmed() != null && order.getPickupConfirmed() == 1) {
            throw new RuntimeException("已确认取车，无需重复确认");
        }

        // 记录取车确认时间
        order.setPickupConfirmed(1);
        order.setPickupTime(LocalDateTime.now());
        orderMapper.updateById(order);

        return order;
    }

    /**
     * 检查未取车的订单（定时任务调用）
     * - 超过2小时：发送警告邮件
     * - 超过24小时：自动取消订单
     */
    @Transactional
    public void checkUnconfirmedPickups() {
        // 查找所有在租/预约中但未确认取车的订单
        List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .in(Order::getStatus, 1, 4)
                        .eq(Order::getDepositPaid, 1)
                        .and(q -> q.isNull(Order::getPickupConfirmed).or().eq(Order::getPickupConfirmed, 0))
        );

        for (Order order : orders) {
            LocalDateTime depositTime = order.getDepositPaidTime();
            if (depositTime == null) continue;

            long hoursSinceDeposit = java.time.temporal.ChronoUnit.HOURS.between(depositTime, LocalDateTime.now());

            // 超过24小时：自动取消订单
            if (hoursSinceDeposit >= 24) {
                try {
                    cancelOrderInternal(order);
                    // 发送取消通知邮件
                    User user = userService.getUserById(order.getUserId());
                    if (user != null && user.getEmail() != null) {
                        sendCancelEmail(user, order);
                    }
                } catch (Exception e) {
                    // 取消失败，记录日志但不影响其他订单处理
                }
                continue;
            }

            // 超过2小时：发送警告邮件
            if (hoursSinceDeposit >= 2 && (order.getPickupWarningSent() == null || order.getPickupWarningSent() == 0)) {
                try {
                    User user = userService.getUserById(order.getUserId());
                    if (user != null && user.getEmail() != null) {
                        sendPickupWarningEmail(user, order);
                        order.setPickupWarningSent(1);
                        orderMapper.updateById(order);
                    }
                } catch (Exception e) {
                    // 发送失败，记录日志但不影响其他订单处理
                }
            }
        }
    }

    /**
     * 内部取消订单方法（用于自动取消）
     */
    private void cancelOrderInternal(Order order) {
        order.setStatus(3);
        order.setCancelTime(LocalDateTime.now());
        order.setDepositRefund(order.getDeposit());
        orderMapper.updateById(order);
        carService.updateStatus(order.getCarId(), 0);
        if (order.getDriverId() != null) {
            driverService.updateStatus(order.getDriverId(), 0);
        }
    }

    /**
     * 发送取车警告邮件
     */
    private void sendPickupWarningEmail(User user, Order order) {
        String subject = "【御途租车】取车确认提醒";
        String content = String.format(
                "尊敬的用户 %s：\n\n" +
                "您有一笔订单（订单号：%s）尚未确认取车。\n\n" +
                "订单详情：\n" +
                "- 车辆：%s %s\n" +
                "- 押金支付时间：%s\n\n" +
                "请在24小时内确认取车，否则订单将自动取消。\n\n" +
                "如需帮助，请联系客服：400-888-8888",
                user.getNickname() != null ? user.getNickname() : user.getUsername(),
                order.getOrderNo(),
                order.getCar() != null ? order.getCar().getBrand() : "",
                order.getCar() != null ? order.getCar().getModel() : "",
                order.getDepositPaidTime()
        );
        // 实际发送邮件（这里简化处理，实际应使用JavaMailSender）
        // emailService.send(user.getEmail(), subject, content);
    }

    /**
     * 发送订单取消通知邮件
     */
    private void sendCancelEmail(User user, Order order) {
        String subject = "【御途租车】订单已自动取消";
        String content = String.format(
                "尊敬的用户 %s：\n\n" +
                "您的订单（订单号：%s）因超过24小时未确认取车，已自动取消。\n\n" +
                "押金 ¥%s 将在1-3个工作日内原路退回。\n\n" +
                "如需重新下单，请访问我们的网站。\n\n" +
                "感谢您的使用！",
                user.getNickname() != null ? user.getNickname() : user.getUsername(),
                order.getOrderNo(),
                order.getDeposit()
        );
        // 实际发送邮件（这里简化处理，实际应使用JavaMailSender）
        // emailService.send(user.getEmail(), subject, content);
    }

    @Transactional
    public Order returnCar(Long orderId, Long userId) {
        return returnCar(orderId, userId, false);
    }

    @Transactional
    public Order returnCar(Long orderId, Long userId, boolean earlyReturn) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) throw new RuntimeException("订单不存在");
        if (!order.getUserId().equals(userId)) throw new RuntimeException("无权操作此订单");
        if (order.getStatus() != 1) {
            throw new RuntimeException("订单状态不允许归还");
        }

        LocalDateTime returnTime = LocalDateTime.now();
        order.setActualReturnTime(returnTime);

        // 计算实际租期（精确到0.01天）
        long totalMinutes = Duration.between(order.getStartTime(), returnTime).toMinutes();
        BigDecimal actualDays = BigDecimal.valueOf(totalMinutes).divide(BigDecimal.valueOf(1440), 2, java.math.RoundingMode.HALF_UP);
        if (actualDays.compareTo(BigDecimal.ONE) < 0) actualDays = BigDecimal.ONE;

        Car car = carService.getById(order.getCarId());
        BigDecimal actualCarCost = car.getPricePerDay().multiply(actualDays);
        BigDecimal actualDriverCost = BigDecimal.ZERO;
        if (order.getDriverId() != null) {
            actualDriverCost = DRIVER_DAILY_FEE.multiply(actualDays);
        }

        // 新用户优惠：还车时按原订单折扣比例重新计算
        if (order.getDiscount() != null && order.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal originalCarCost = car.getPricePerDay().multiply(
                    BigDecimal.valueOf(Duration.between(order.getStartTime(), order.getEndTime()).toMinutes())
                            .divide(BigDecimal.valueOf(1440), 2, java.math.RoundingMode.HALF_UP));
            BigDecimal originalDriverCost = order.getDriverId() != null ? DRIVER_DAILY_FEE.multiply(
                    BigDecimal.valueOf(Duration.between(order.getStartTime(), order.getEndTime()).toMinutes())
                            .divide(BigDecimal.valueOf(1440), 2, java.math.RoundingMode.HALF_UP)) : BigDecimal.ZERO;
            BigDecimal originalTotalForDiscount = originalCarCost.add(originalDriverCost);
            if (originalTotalForDiscount.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal discountRatio = order.getDiscount().divide(originalTotalForDiscount, 4, java.math.RoundingMode.HALF_UP);
                BigDecimal actualDiscount = actualCarCost.add(actualDriverCost).multiply(discountRatio).min(new BigDecimal("200"));
                actualCarCost = actualCarCost.subtract(actualDiscount);
                order.setDiscount(actualDiscount);
            }
        }

        BigDecimal originalTotal = order.getTotalCost();
        BigDecimal actualTotal = actualCarCost.add(actualDriverCost);
        BigDecimal depositRefund = BigDecimal.ZERO;

        if (earlyReturn) {
            // 提前归还：租金不退，只退押金
            depositRefund = order.getDeposit();
            order.setTotalCost(originalTotal); // 保持原订单费用不变
        } else {
            // 正常还车：提前还车退还差额，超时不额外收费
            if (actualTotal.compareTo(originalTotal) < 0) {
                depositRefund = originalTotal.subtract(actualTotal);
            }
            BigDecimal totalRefund = order.getDeposit().add(depositRefund);
            order.setTotalCost(actualTotal);
            order.setDepositRefund(totalRefund);
        }

        order.setStatus(2); // 已完成

        // === 里程记录 ===
        int startMileage = order.getStartMileage() != null ? order.getStartMileage() : car.getMileage();
        // 根据租期和车辆类型模拟行驶里程
        int mileageDriven = estimateMileage(car, actualDays.longValue());
        int endMileage = startMileage + mileageDriven;
        order.setStartMileage(startMileage);
        order.setEndMileage(endMileage);
        order.setMileageDriven(mileageDriven);

        orderMapper.updateById(order);

        // 更新车辆总里程
        car.setMileage(endMileage);
        carService.updateMileage(car.getId(), endMileage);

        // 增量更新RAG知识库
        try {
            ragService.addMileageEntry(order, car);
        } catch (Exception e) {
            // RAG更新失败不影响还车主流程
        }

        // 更新车辆状态
        carService.updateStatus(order.getCarId(), 0);

        // 更新司机状态
        if (order.getDriverId() != null) {
            driverService.updateStatus(order.getDriverId(), 0); // 设为空闲
            driverService.incrementServiceCount(order.getDriverId());
        }

        return order;
    }

    @Transactional
    public Order cancelOrder(Long orderId, Long userId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) throw new RuntimeException("订单不存在");
        if (!order.getUserId().equals(userId)) throw new RuntimeException("无权操作此订单");

        // 待支付订单（status=0）可随时取消
        if (order.getStatus() == 0) {
            order.setStatus(3);
            order.setCancelTime(LocalDateTime.now());
            order.setDepositRefund(BigDecimal.ZERO);
            orderMapper.updateById(order);
            return order;
        }

        // 已支付押金的预约/租车订单需要检查时间窗口
        if (order.getStatus() != 1 && order.getStatus() != 4) {
            throw new RuntimeException("订单状态不允许取消");
        }
        if (order.getDepositPaid() == null || order.getDepositPaid() != 1) {
            throw new RuntimeException("押金未支付，无法取消");
        }

        // 检查每日取消限制
        if (hasCancelledToday(order.getUserId())) {
            throw new RuntimeException("一天内最多可以取消一次订单");
        }

        // 检查时间窗口
        LocalDateTime depositTime = order.getDepositPaidTime();
        if (depositTime == null) {
            throw new RuntimeException("押金支付记录异常");
        }
        LocalDateTime now = LocalDateTime.now();
        long minutesSinceDeposit = Duration.between(depositTime, now).toMinutes();

        if (order.getStatus() == 4) {
            // 预约订单：2小时内可取消
            if (minutesSinceDeposit > 120) {
                throw new RuntimeException("预约订单支付押金后2小时内可取消，已超过取消时限");
            }
        } else if (order.getStatus() == 1) {
            // 租车订单：1小时内可取消
            if (minutesSinceDeposit > 60) {
                throw new RuntimeException("租车订单支付押金后1小时内可取消，已超过取消时限");
            }
        }

        order.setStatus(3);
        order.setCancelTime(LocalDateTime.now());
        order.setDepositRefund(order.getDeposit()); // 退还全部押金
        orderMapper.updateById(order);
        carService.updateStatus(order.getCarId(), 0);

        // 释放司机
        if (order.getDriverId() != null) {
            driverService.updateStatus(order.getDriverId(), 0);
        }

        return order;
    }

    /**
     * 获取订单取消信息（倒计时、是否可取消等）
     */
    public Map<String, Object> getCancelInfo(Long orderId, Long userId) {
        Order order = orderMapper.selectById(orderId);
        Map<String, Object> info = new HashMap<>();
        if (order == null) {
            info.put("cancellable", false);
            info.put("reason", "订单不存在");
            return info;
        }

        boolean alreadyCancelledToday = hasCancelledToday(userId);
        info.put("alreadyCancelledToday", alreadyCancelledToday);

        // 待支付订单：可随时取消
        if (order.getStatus() == 0) {
            info.put("cancellable", true);
            info.put("remainSeconds", -1); // 无时间限制
            info.put("orderStatus", 0);
            return info;
        }

        // 只有已支付押金的预约/租车订单才显示取消倒计时
        if ((order.getStatus() == 1 || order.getStatus() == 4) && order.getDepositPaid() != null && order.getDepositPaid() == 1) {
            LocalDateTime depositTime = order.getDepositPaidTime();
            if (depositTime != null) {
                int windowMinutes = order.getStatus() == 4 ? 120 : 60; // 预约2h，租车1h
                LocalDateTime deadline = depositTime.plusMinutes(windowMinutes);
                long remainSeconds = Duration.between(LocalDateTime.now(), deadline).toSeconds();

                info.put("orderStatus", order.getStatus());
                info.put("depositPaidTime", depositTime.toString());
                info.put("windowMinutes", windowMinutes);

                if (remainSeconds > 0) {
                    info.put("cancellable", !alreadyCancelledToday);
                    info.put("remainSeconds", remainSeconds);
                } else {
                    info.put("cancellable", false);
                    info.put("remainSeconds", 0);
                    info.put("reason", alreadyCancelledToday ? "今日已取消过一次订单" : "已超过取消时限");
                }
                return info;
            }
        }

        info.put("cancellable", false);
        info.put("remainSeconds", 0);
        info.put("orderStatus", order.getStatus());
        return info;
    }

    /**
     * 检查用户今天是否已取消过订单
     */
    private boolean hasCancelledToday(Long userId) {
        LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
        Long count = orderMapper.selectCount(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getUserId, userId)
                        .eq(Order::getStatus, 3)
                        .ge(Order::getCancelTime, todayStart));
        return count > 0;
    }

    @Transactional
    public Order rateOrder(Long orderId, Long userId, Integer rating, String comment) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) throw new RuntimeException("订单不存在");
        if (!order.getUserId().equals(userId)) throw new RuntimeException("无权操作此订单");
        if (order.getStatus() != 2) {
            throw new RuntimeException("只能评价已完成的订单");
        }
        if (rating < 1 || rating > 5) {
            throw new RuntimeException("评分必须在1-5之间");
        }
        order.setUserRating(rating);
        order.setUserComment(comment);
        orderMapper.updateById(order);
        return order;
    }

    // 检查是否新用户（注册30天内，且未使用过首单优惠）
    private boolean isNewUser(Long userId) {
        // 查询用户是否有已完成或进行中的订单（排除已取消的订单）
        Order existingOrder = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getUserId, userId)
                        .ne(Order::getStatus, 3) // 排除已取消订单
                        .orderByAsc(Order::getCreateTime)
                        .last("LIMIT 1"));
        if (existingOrder != null) return false; // 已有有效订单，不是新用户

        // 检查注册时间是否在30天内
        User user = userService.getUserById(userId);
        if (user == null || user.getCreateTime() == null) return false;
        long daysSinceRegister = Duration.between(user.getCreateTime(), LocalDateTime.now()).toDays();
        return daysSinceRegister <= 30; // 注册30天内
    }

    // 公开方法：检查用户是否可以使用新用户优惠（供Controller调用）
    public boolean checkNewUserCoupon(Long userId) {
        return isNewUser(userId);
    }

    // 重置用户优惠券（将所有非取消订单标记为已取消）
    public int resetCoupon(Long userId) {
        List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getUserId, userId)
                        .ne(Order::getStatus, 3));
        int count = 0;
        for (Order order : orders) {
            order.setStatus(3);
            order.setCancelTime(LocalDateTime.now());
            orderMapper.updateById(order);
            count++;
        }
        return count;
    }

    // 重置用户优惠券 + 刷新注册时间到当前（确保30天内）
    public int resetCouponWithRefresh(Long userId, com.carrental.entity.User user) {
        int count = resetCoupon(userId);
        // 更新注册时间为当前，确保30天有效期
        user.setCreateTime(LocalDateTime.now());
        userService.updateUserCreateTime(user);
        return count;
    }

    // 刷新指定订单的取消窗口（重置depositPaidTime为当前时间）
    public void refreshCancelWindow(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order != null && (order.getStatus() == 1 || order.getStatus() == 4)) {
            order.setDepositPaidTime(LocalDateTime.now());
            orderMapper.updateById(order);
        }
    }

    // 恢复被误取消的订单到正确状态
    public void restoreOrder(Long orderId, int newStatus) {
        Order order = orderMapper.selectById(orderId);
        if (order != null && order.getStatus() == 3) {
            order.setStatus(newStatus);
            order.setCancelTime(null);
            // 如果恢复为在租/预约状态，刷新押金支付时间以重置取消窗口
            if (newStatus == 1 || newStatus == 4) {
                order.setDepositPaidTime(LocalDateTime.now());
            }
            orderMapper.updateById(order);
        }
    }

    // 根据车辆类型和租期估算行驶里程
    private int estimateMileage(Car car, long days) {
        // 基础日均里程
        int baseDaily;
        String category = car.getCategory();
        String usageType = car.getUsageType() != null ? car.getUsageType() : "";

        if (usageType.contains("旅游") || usageType.contains("家庭")) {
            baseDaily = 180; // 旅游/家庭出行里程多
        } else if (usageType.contains("商务")) {
            baseDaily = 100; // 商务接待以市区为主
        } else if ("新能源".equals(category)) {
            baseDaily = 90;  // 新能源通勤为主
        } else {
            baseDaily = 120; // 默认
        }

        // 加入随机波动 ±30%
        double factor = 0.7 + ThreadLocalRandom.current().nextDouble() * 0.6;
        return (int) (baseDaily * days * factor);
    }

    // 检查时间段冲突
    private boolean hasTimeConflict(Long carId, LocalDateTime start, LocalDateTime end, Long excludeOrderId) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getCarId, carId)
                .in(Order::getStatus, 1, 4) // 在租或预约中
                .lt(Order::getStartTime, end)
                .gt(Order::getEndTime, start);
        if (excludeOrderId != null) {
            wrapper.ne(Order::getId, excludeOrderId);
        }
        return orderMapper.selectCount(wrapper) > 0;
    }
}
