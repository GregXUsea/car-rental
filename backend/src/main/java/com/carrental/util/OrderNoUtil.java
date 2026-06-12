package com.carrental.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

public class OrderNoUtil {
    private static final AtomicInteger counter = new AtomicInteger(0);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public static String generate() {
        String time = LocalDateTime.now().format(FMT);
        int seq = counter.incrementAndGet() % 10000;
        return "ORD" + time + String.format("%04d", seq);
    }
}
