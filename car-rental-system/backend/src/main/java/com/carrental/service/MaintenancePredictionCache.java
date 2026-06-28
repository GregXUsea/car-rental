package com.carrental.service;

import com.carrental.entity.Car;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 维护预测缓存服务
 * - 支持 AI 模式（DeepSeek API）和本地模式（规则引擎）
 * - 每天凌晨3点批量跑全量车辆预测，结果缓存到内存
 * - 看板接口直接读缓存，秒开
 * - 支持手动触发刷新
 */
@Service
public class MaintenancePredictionCache {

    private static final Logger log = LoggerFactory.getLogger(MaintenancePredictionCache.class);

    @Autowired
    private AIService aiService;

    @Autowired
    private CarService carService;

    // carId -> 预测结果（含car信息和prediction）
    private final ConcurrentHashMap<Long, Map<String, Object>> cache = new ConcurrentHashMap<>();

    // 全量预测结果列表（按风险等级排序）
    private volatile List<Map<String, Object>> allPredictions = Collections.emptyList();

    private volatile Date lastRefreshedAt;
    private volatile boolean refreshing = false;
    private volatile int refreshProgress = 0;
    private volatile int refreshTotal = 0;

    // AI模式标志：true=使用DeepSeek API，false=本地规则引擎
    // 默认本地模式（启动快），管理员可手动切换到AI模式
    private volatile boolean aiMode = false;

    // 预测来源标记
    private volatile String lastSource = "本地";

    @PostConstruct
    public void init() {
        // 启动后立即异步执行首次预测
        Thread.startVirtualThread(() -> {
            try {
                Thread.sleep(5000);
                refreshAll();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    /**
     * 定时任务：每天凌晨3点全量刷新预测
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void scheduledRefresh() {
        log.info("[缓存] 定时任务触发，开始全量刷新维护预测...");
        refreshAll();
    }

    /**
     * 获取单辆车的预测（优先缓存，未命中时实时预测）
     */
    public Map<String, Object> getPrediction(Long carId) {
        Map<String, Object> cached = cache.get(carId);
        if (cached != null) return cached;

        Car car = carService.getById(carId);
        if (car == null) return null;

        Map<String, Object> item = new HashMap<>();
        item.put("car", car);
        try {
            if (aiMode) {
                item.put("prediction", aiService.getMaintenancePrediction(carId));
            } else {
                item.put("prediction", aiService.getQuickPrediction(carId));
            }
        } catch (Exception e) {
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("nextMaintenanceDate", "预测失败");
            fallback.put("nextMaintenanceType", "未知");
            fallback.put("suggestions", List.of("暂无建议"));
            fallback.put("riskLevel", "中");
            fallback.put("source", "本地");
            item.put("prediction", fallback);
        }

        cache.put(carId, item);
        return item;
    }

    /**
     * 获取全量预测结果（已排序）
     */
    public List<Map<String, Object>> getAllPredictions() {
        return allPredictions;
    }

    /**
     * 设置AI模式
     */
    public void setAiMode(boolean enabled) {
        this.aiMode = enabled;
        log.info("[缓存] 切换到{}模式", enabled ? "AI(DeepSeek)" : "本地规则");
    }

    public boolean isAiMode() {
        return aiMode;
    }

    /**
     * 手动触发全量刷新
     */
    public Map<String, Object> refreshAll() {
        return refreshAll(this.aiMode);
    }

    /**
     * 手动触发全量刷新（指定模式）
     * AI模式下并行调用 DeepSeek API，大幅提速
     */
    public Map<String, Object> refreshAll(boolean useAi) {
        if (refreshing) {
            Map<String, Object> status = new HashMap<>();
            status.put("status", "正在刷新中");
            status.put("progress", refreshProgress);
            status.put("total", refreshTotal);
            return status;
        }

        this.aiMode = useAi;
        refreshing = true;
        lastSource = useAi ? "AI" : "本地";
        long startTime = System.currentTimeMillis();

        try {
            List<Car> cars = carService.listAll();
            refreshTotal = cars.size();
            refreshProgress = 0;

            log.info("[缓存] 开始刷新 {} 辆车的维护预测... (模式: {})", refreshTotal, useAi ? "AI" : "本地");

            AtomicInteger aiCount = new AtomicInteger(0);
            AtomicInteger localCount = new AtomicInteger(0);

            if (useAi) {
                // ===== AI模式：并行调用 DeepSeek API（最多5个并发） =====
                log.info("[缓存] AI模式：并行调用 DeepSeek API，最多10个并发...");
                List<java.util.concurrent.Future<Map<String, Object>>> futures = new ArrayList<>();
                java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(10);

                for (Car car : cars) {
                    final Long carId = car.getId();
                    futures.add(executor.submit(() -> {
                        Map<String, Object> item = new HashMap<>();
                        item.put("car", car);
                        try {
                            Map<String, Object> prediction = aiService.getMaintenancePrediction(carId);
                            item.put("prediction", prediction);
                            aiCount.incrementAndGet();
                        } catch (Exception e) {
                            log.warn("[缓存] 车辆 {} AI预测失败: {}", carId, e.getMessage());
                            item.put("prediction", buildFallbackMap("中"));
                            localCount.incrementAndGet();
                        }
                        refreshProgress++;
                        cache.put(carId, item);
                        return item;
                    }));
                }

                // 收集结果
                List<Map<String, Object>> results = new ArrayList<>();
                for (java.util.concurrent.Future<Map<String, Object>> future : futures) {
                    try {
                        results.add(future.get(120, java.util.concurrent.TimeUnit.SECONDS));
                    } catch (Exception e) {
                        log.warn("[缓存] 获取预测结果失败: {}", e.getMessage());
                    }
                }
                executor.shutdown();

                // 按风险等级排序
                results.sort((a, b) -> riskPriority(getRiskLevel(b)) - riskPriority(getRiskLevel(a)));
                allPredictions = results;

            } else {
                // ===== 本地模式：串行（毫秒级，无需并行） =====
                List<Map<String, Object>> results = new ArrayList<>();
                for (Car car : cars) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("car", car);
                    try {
                        item.put("prediction", aiService.getQuickPrediction(car.getId()));
                        localCount.incrementAndGet();
                    } catch (Exception e) {
                        item.put("prediction", buildFallbackMap("中"));
                        localCount.incrementAndGet();
                    }
                    results.add(item);
                    cache.put(car.getId(), item);
                    refreshProgress++;
                }
                results.sort((a, b) -> riskPriority(getRiskLevel(b)) - riskPriority(getRiskLevel(a)));
                allPredictions = results;
            }

            lastRefreshedAt = new Date();
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("[缓存] 全量刷新完成，共 {} 辆车 (AI:{}, 本地:{})，耗时 {}s",
                    refreshTotal, aiCount.get(), localCount.get(), elapsed / 1000);

            Map<String, Object> status = new HashMap<>();
            status.put("status", "完成");
            status.put("count", refreshTotal);
            status.put("aiCount", aiCount.get());
            status.put("localCount", localCount.get());
            status.put("elapsed", elapsed);
            status.put("lastRefreshedAt", lastRefreshedAt);
            status.put("source", lastSource);
            status.put("aiMode", useAi);
            return status;

        } finally {
            refreshing = false;
        }
    }

    private Map<String, Object> buildFallbackMap(String riskLevel) {
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("nextMaintenanceDate", "预测失败");
        fallback.put("nextMaintenanceType", "未知");
        fallback.put("suggestions", List.of("暂无建议"));
        fallback.put("riskLevel", riskLevel);
        fallback.put("source", "本地");
        return fallback;
    }

    /**
     * 获取缓存状态
     */
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("cachedCount", cache.size());
        status.put("lastRefreshedAt", lastRefreshedAt);
        status.put("refreshing", refreshing);
        status.put("progress", refreshProgress);
        status.put("total", refreshTotal);
        status.put("aiMode", aiMode);
        status.put("source", lastSource);
        return status;
    }

    @SuppressWarnings("unchecked")
    private String getRiskLevel(Map<String, Object> item) {
        Map<String, Object> pred = (Map<String, Object>) item.get("prediction");
        if (pred == null || pred.get("riskLevel") == null) return "中";
        String level = String.valueOf(pred.get("riskLevel"));
        if (level.contains("高")) return "高";
        if (level.contains("低")) return "低";
        return "中";
    }

    private int riskPriority(String level) {
        return switch (level) {
            case "高" -> 0;
            case "中" -> 1;
            case "低" -> 2;
            default -> 1;
        };
    }
}
