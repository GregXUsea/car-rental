package com.carrental.util;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 验证码内存存储器（演示用途，生产环境应使用 Redis）
 * 验证码有效期 5 分钟，每个邮箱 60 秒内不可重复发送
 */
@Component
public class VerificationCodeStore {

    /** 验证码有效期：5分钟 */
    private static final long CODE_EXPIRE_MS = 5 * 60 * 1000;
    /** 发送间隔：60秒 */
    private static final long SEND_INTERVAL_MS = 60 * 1000;

    private final ConcurrentHashMap<String, CodeEntry> store = new ConcurrentHashMap<>();

    /**
     * 生成并存储验证码
     * @return 生成的 6 位验证码
     */
    public String generateAndStore(String key) {
        CodeEntry existing = store.get(key);
        if (existing != null && !existing.isExpired()) {
            long elapsed = System.currentTimeMillis() - existing.createTime;
            if (elapsed < SEND_INTERVAL_MS) {
                return null; // 发送太频繁
            }
        }
        String code = String.format("%06d", (int) (Math.random() * 1000000));
        store.put(key, new CodeEntry(code, System.currentTimeMillis()));
        System.out.println("========================================");
        System.out.println("【验证码】邮箱/手机 " + key + " 的验证码为: " + code);
        System.out.println("【验证码】有效期 5 分钟，请尽快使用");
        System.out.println("========================================");
        return code;
    }

    /**
     * 验证验证码是否正确且未过期
     */
    public boolean verify(String key, String code) {
        CodeEntry entry = store.get(key);
        if (entry == null) return false;
        if (entry.isExpired()) {
            store.remove(key);
            return false;
        }
        if (!entry.code.equals(code)) return false;
        store.remove(key); // 验证成功后删除，防止重复使用
        return true;
    }

    /**
     * 检查是否在发送冷却期内
     */
    public boolean canSend(String key) {
        CodeEntry entry = store.get(key);
        if (entry == null) return true;
        if (entry.isExpired()) return true;
        return System.currentTimeMillis() - entry.createTime >= SEND_INTERVAL_MS;
    }

    private static class CodeEntry {
        final String code;
        final long createTime;

        CodeEntry(String code, long createTime) {
            this.code = code;
            this.createTime = createTime;
        }

        boolean isExpired() {
            return System.currentTimeMillis() - createTime > CODE_EXPIRE_MS;
        }
    }
}
