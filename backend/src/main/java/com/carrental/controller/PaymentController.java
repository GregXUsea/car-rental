package com.carrental.controller;

import com.carrental.dto.Result;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 模拟支付控制器
 */
@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    // 存储支付验证码和确认状态
    private static final Map<String, Boolean> paymentStatus = new ConcurrentHashMap<>();

    /**
     * 确认支付（手机端调用）
     */
    @PostMapping("/confirm")
    public Result<Void> confirmPayment(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        if (code == null || code.isEmpty()) {
            return Result.error("验证码不能为空");
        }
        paymentStatus.put(code, true);
        return Result.success(null);
    }

    /**
     * 查询支付状态（电脑端轮询）
     */
    @GetMapping("/status/{code}")
    public Result<Boolean> getPaymentStatus(@PathVariable String code) {
        Boolean status = paymentStatus.get(code);
        return Result.success(status != null && status);
    }
}
