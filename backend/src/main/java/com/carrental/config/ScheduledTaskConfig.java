package com.carrental.config;

import com.carrental.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 定时任务配置
 * 检查未取车的订单，发送警告或自动取消
 */
@Configuration
@EnableScheduling
public class ScheduledTaskConfig {

    @Autowired
    private OrderService orderService;

    /**
     * 每30分钟检查一次未取车的订单
     * - 超过2小时：发送警告邮件
     * - 超过24小时：自动取消订单
     */
    @Scheduled(fixedRate = 30 * 60 * 1000) // 30分钟
    public void checkUnconfirmedPickups() {
        try {
            orderService.checkUnconfirmedPickups();
        } catch (Exception e) {
            // 记录日志，不影响系统运行
        }
    }
}
