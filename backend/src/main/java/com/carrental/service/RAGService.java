package com.carrental.service;

import com.carrental.entity.Car;
import com.carrental.entity.MaintenanceRecord;
import com.carrental.entity.Order;
import com.carrental.mapper.MaintenanceRecordMapper;
import com.carrental.mapper.OrderMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RAG 知识库服务
 * - 将保养记录向量化存入内存知识库
 * - 提供余弦相似度检索，为每辆车找到最相似的历史维护案例
 * - 定时任务自动更新知识库
 */
@Service
public class RAGService {

    private static final Logger log = LoggerFactory.getLogger(RAGService.class);

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private MaintenanceRecordMapper maintenanceRecordMapper;

    @Autowired
    private CarService carService;

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 知识库条目：保养记录 + 向量 + 元数据
     */
    public static class KnowledgeEntry {
        private Long recordId;
        private Long carId;
        private String carName;          // "丰田 卡罗拉"
        private String text;             // 用于检索的文本摘要
        private float[] embedding;       // 向量
        private int mileage;
        private String maintenanceType;
        private String description;
        private String date;
        private double cost;
        private Integer mileageDriven;  // 本次行驶里程

        // getters & setters
        public Long getRecordId() { return recordId; }
        public void setRecordId(Long recordId) { this.recordId = recordId; }
        public Long getCarId() { return carId; }
        public void setCarId(Long carId) { this.carId = carId; }
        public String getCarName() { return carName; }
        public void setCarName(String carName) { this.carName = carName; }
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public float[] getEmbedding() { return embedding; }
        public void setEmbedding(float[] embedding) { this.embedding = embedding; }
        public int getMileage() { return mileage; }
        public void setMileage(int mileage) { this.mileage = mileage; }
        public String getMaintenanceType() { return maintenanceType; }
        public void setMaintenanceType(String maintenanceType) { this.maintenanceType = maintenanceType; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public double getCost() { return cost; }
        public void setCost(double cost) { this.cost = cost; }
        public Integer getMileageDriven() { return mileageDriven; }
        public void setMileageDriven(Integer mileageDriven) { this.mileageDriven = mileageDriven; }
    }

    /**
     * 检索结果：知识条目 + 相似度分数
     */
    public static class RetrievalResult {
        private KnowledgeEntry entry;
        private double similarity;

        public RetrievalResult(KnowledgeEntry entry, double similarity) {
            this.entry = entry;
            this.similarity = similarity;
        }

        public KnowledgeEntry getEntry() { return entry; }
        public double getSimilarity() { return similarity; }
    }

    // 内存知识库：recordId -> KnowledgeEntry
    private final ConcurrentHashMap<Long, KnowledgeEntry> knowledgeBase = new ConcurrentHashMap<>();

    // 最近一次索引时间
    private volatile Date lastIndexedAt;

    // 是否正在索引
    private volatile boolean indexing = false;

    @PostConstruct
    public void init() {
        // 启动时尝试加载知识库（异步，不阻塞启动）
        Thread.startVirtualThread(this::buildKnowledgeBase);
    }

    /**
     * 定时任务：每天凌晨2点重建知识库
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduledReindex() {
        log.info("[RAG] 定时任务触发，开始重建知识库...");
        buildKnowledgeBase();
    }

    /**
     * 手动触发重建知识库
     */
    public Map<String, Object> rebuildKnowledgeBase() {
        buildKnowledgeBase();
        Map<String, Object> result = new HashMap<>();
        result.put("entryCount", knowledgeBase.size());
        result.put("lastIndexedAt", lastIndexedAt);
        result.put("embeddingAvailable", embeddingService.isAvailable());
        return result;
    }

    /**
     * 构建知识库：加载所有保养记录 -> 向量化 -> 存入内存
     */
    public void buildKnowledgeBase() {
        if (indexing) {
            log.info("[RAG] 知识库正在索引中，跳过重复调用");
            return;
        }
        indexing = true;
        try {
            log.info("[RAG] 开始构建知识库...");

            // 1. 加载所有保养记录
            List<MaintenanceRecord> records = maintenanceRecordMapper.selectList(null);

            // 2. 加载车辆信息用于构建文本
            Map<Long, Car> carMap = new HashMap<>();
            List<Car> cars = carService.listAll();
            for (Car car : cars) {
                carMap.put(car.getId(), car);
            }

            // 3. 构建文本摘要（保养记录 + 里程记录）
            List<String> texts = new ArrayList<>();
            List<String> entryTypes = new ArrayList<>(); // "maintenance" or "mileage"
            List<MaintenanceRecord> validRecords = new ArrayList<>();
            List<Order> validOrders = new ArrayList<>();

            // 3a. 保养记录
            if (records.isEmpty()) {
                log.info("[RAG] 无保养记录，仅索引里程数据");
            }
            for (MaintenanceRecord record : records) {
                Car car = carMap.get(record.getCarId());
                if (car == null) continue;

                String text = buildRecordText(record, car);
                texts.add(text);
                entryTypes.add("maintenance");
                validRecords.add(record);
            }

            // 3b. 已完成订单的里程记录
            List<Order> completedOrders = orderMapper.selectList(
                    new LambdaQueryWrapper<Order>()
                            .eq(Order::getStatus, 2)
                            .isNotNull(Order::getMileageDriven)
                            .gt(Order::getMileageDriven, 0));
            for (Order order : completedOrders) {
                Car car = carMap.get(order.getCarId());
                if (car == null) continue;

                String text = buildOrderMileageText(order, car);
                texts.add(text);
                entryTypes.add("mileage");
                validOrders.add(order);
            }

            // 4. 向量化（如果没有任何数据则跳过）
            if (texts.isEmpty()) {
                log.info("[RAG] 无可用数据，跳过知识库构建");
                return;
            }
            List<float[]> embeddings;
            if (embeddingService.isAvailable()) {
                log.info("[RAG] 使用 OpenAI Embeddings API 向量化 {} 条记录...", texts.size());
                // 分批处理，每批最多 20 条
                embeddings = new ArrayList<>();
                for (int i = 0; i < texts.size(); i += 20) {
                    int end = Math.min(i + 20, texts.size());
                    List<String> batch = texts.subList(i, end);
                    try {
                        List<float[]> batchEmbeddings = embeddingService.embedBatch(batch);
                        embeddings.addAll(batchEmbeddings);
                    } catch (IOException e) {
                        log.warn("[RAG] 批次向量化失败 ({}-{}), 使用本地特征向量: {}", i, end, e.getMessage());
                        for (String text : batch) {
                            embeddings.add(localFeatureVector(text));
                        }
                    }
                }
            } else {
                log.info("[RAG] OpenAI API 不可用，使用本地特征向量化 {} 条记录", texts.size());
                embeddings = new ArrayList<>();
                for (String text : texts) {
                    embeddings.add(localFeatureVector(text));
                }
            }

            // 5. 存入知识库
            knowledgeBase.clear();
            int textIdx = 0;

            // 5a. 保养记录条目
            for (MaintenanceRecord record : validRecords) {
                Car car = carMap.get(record.getCarId());
                KnowledgeEntry entry = new KnowledgeEntry();
                entry.setRecordId(record.getId());
                entry.setCarId(record.getCarId());
                entry.setCarName(car.getBrand() + " " + car.getModel());
                entry.setText(texts.get(textIdx));
                entry.setEmbedding(embeddings.get(textIdx));
                entry.setMileage(record.getMileageAtMaintenance());
                entry.setMaintenanceType(record.getMaintenanceType());
                entry.setDescription(record.getDescription());
                entry.setDate(record.getMaintenanceDate() != null ? record.getMaintenanceDate().toString() : "未知");
                entry.setCost(record.getCost() != null ? record.getCost().doubleValue() : 0);
                knowledgeBase.put(record.getId(), entry);
                textIdx++;
            }

            // 5b. 里程记录条目（用负数ID区分，避免与保养记录冲突）
            for (Order order : validOrders) {
                Car car = carMap.get(order.getCarId());
                KnowledgeEntry entry = new KnowledgeEntry();
                entry.setRecordId(-order.getId()); // 负数ID标识里程记录
                entry.setCarId(order.getCarId());
                entry.setCarName(car.getBrand() + " " + car.getModel());
                entry.setText(texts.get(textIdx));
                entry.setEmbedding(embeddings.get(textIdx));
                entry.setMileage(order.getEndMileage());
                entry.setMaintenanceType("租赁行程");
                entry.setDescription(String.format("行驶%dkm，%s至%s",
                        order.getMileageDriven(),
                        order.getStartTime() != null ? order.getStartTime().toLocalDate().toString() : "",
                        order.getActualReturnTime() != null ? order.getActualReturnTime().toLocalDate().toString() : ""));
                entry.setDate(order.getActualReturnTime() != null ? order.getActualReturnTime().toLocalDate().toString() : "未知");
                entry.setCost(order.getTotalCost() != null ? order.getTotalCost().doubleValue() : 0);
                entry.setMileageDriven(order.getMileageDriven());
                knowledgeBase.put(entry.getRecordId(), entry);
                textIdx++;
            }

            lastIndexedAt = new Date();
            log.info("[RAG] 知识库构建完成，共 {} 条记录", knowledgeBase.size());

        } catch (Exception e) {
            log.error("[RAG] 知识库构建失败: {}", e.getMessage(), e);
        } finally {
            indexing = false;
        }
    }

    /**
     * 检索：为指定车辆找到最相似的历史维护案例（排除自身记录）
     */
    public List<RetrievalResult> retrieveSimilarCases(Long carId, int topK) {
        if (knowledgeBase.isEmpty()) {
            return Collections.emptyList();
        }

        // 构建查询文本：当前车辆信息 + 最近的保养记录
        Car car = carService.getById(carId);
        if (car == null) return Collections.emptyList();

        // 获取该车的保养记录
        List<MaintenanceRecord> carRecords = maintenanceRecordMapper.selectList(
                new LambdaQueryWrapper<MaintenanceRecord>()
                        .eq(MaintenanceRecord::getCarId, carId)
                        .orderByDesc(MaintenanceRecord::getMaintenanceDate)
                        .last("LIMIT 3"));

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(String.format("车辆：%s %s，%s，当前里程%dkm",
                car.getBrand(), car.getModel(), car.getCategory(), car.getMileage()));
        if (car.getLastMaintainDate() != null) {
            queryBuilder.append("，上次保养：").append(car.getLastMaintainDate());
        }
        for (MaintenanceRecord r : carRecords) {
            queryBuilder.append(String.format("。历史保养：%s，%s，%dkm",
                    r.getMaintenanceType(), r.getDescription(), r.getMileageAtMaintenance()));
        }
        String queryText = queryBuilder.toString();

        // 计算查询向量
        float[] queryVector;
        if (embeddingService.isAvailable()) {
            try {
                queryVector = embeddingService.embed(queryText);
            } catch (IOException e) {
                queryVector = localFeatureVector(queryText);
            }
        } else {
            queryVector = localFeatureVector(queryText);
        }

        // 计算与知识库中所有条目的相似度
        List<RetrievalResult> results = new ArrayList<>();
        for (KnowledgeEntry entry : knowledgeBase.values()) {
            // 排除当前车辆自身的记录（找的是其他车辆的相似案例）
            // 但也保留自身车辆的历史记录作为参考
            double sim = EmbeddingService.cosineSimilarity(queryVector, entry.getEmbedding());
            results.add(new RetrievalResult(entry, sim));
        }

        // 按相似度降序排列，取 topK
        results.sort((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()));
        return results.subList(0, Math.min(topK, results.size()));
    }

    /**
     * 获取知识库状态
     */
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("entryCount", knowledgeBase.size());
        status.put("lastIndexedAt", lastIndexedAt);
        status.put("indexing", indexing);
        status.put("embeddingAvailable", embeddingService.isAvailable());
        return status;
    }

    /**
     * 构建保养记录的文本摘要（用于向量化）
     */
    private String buildRecordText(MaintenanceRecord record, Car car) {
        return String.format("%s %s %s，%s保养，里程%dkm，%s，费用%.0f元",
                car.getBrand(), car.getModel(), car.getCategory(),
                record.getMaintenanceType(),
                record.getMileageAtMaintenance(),
                record.getDescription(),
                record.getCost() != null ? record.getCost() : 0);
    }

    /**
     * 构建订单里程记录的文本摘要（用于向量化）
     */
    private String buildOrderMileageText(Order order, Car car) {
        return String.format("%s %s %s，租赁行程，行驶%dkm，取车里程%dkm，还车里程%dkm，租期%d天",
                car.getBrand(), car.getModel(), car.getCategory(),
                order.getMileageDriven() != null ? order.getMileageDriven() : 0,
                order.getStartMileage() != null ? order.getStartMileage() : 0,
                order.getEndMileage() != null ? order.getEndMileage() : 0,
                order.getStartTime() != null && order.getActualReturnTime() != null ?
                        java.time.Duration.between(order.getStartTime(), order.getActualReturnTime()).toDays() : 0);
    }

    /**
     * 增量添加单条知识条目（还车后调用）
     */
    public void addMileageEntry(Order order, Car car) {
        if (order.getMileageDriven() == null || order.getMileageDriven() <= 0) return;

        String text = buildOrderMileageText(order, car);
        float[] embedding;
        if (embeddingService.isAvailable()) {
            try {
                embedding = embeddingService.embed(text);
            } catch (IOException e) {
                embedding = localFeatureVector(text);
            }
        } else {
            embedding = localFeatureVector(text);
        }

        KnowledgeEntry entry = new KnowledgeEntry();
        entry.setRecordId(-order.getId());
        entry.setCarId(order.getCarId());
        entry.setCarName(car.getBrand() + " " + car.getModel());
        entry.setText(text);
        entry.setEmbedding(embedding);
        entry.setMileage(order.getEndMileage());
        entry.setMaintenanceType("租赁行程");
        entry.setDescription(String.format("行驶%dkm", order.getMileageDriven()));
        entry.setDate(order.getActualReturnTime() != null ? order.getActualReturnTime().toLocalDate().toString() : "未知");
        entry.setCost(order.getTotalCost() != null ? order.getTotalCost().doubleValue() : 0);
        entry.setMileageDriven(order.getMileageDriven());

        knowledgeBase.put(entry.getRecordId(), entry);
        log.info("[RAG] 增量添加里程记录：{} {} 行驶{}km", car.getBrand(), car.getModel(), order.getMileageDriven());
    }

    /**
     * 本地特征向量化（当 OpenAI API 不可用时的降级方案）
     * 基于关键词提取生成伪向量，保留基本的语义区分能力
     */
    private float[] localFeatureVector(String text) {
        float[] vector = new float[EmbeddingService.EMBEDDING_DIM];
        String lower = text.toLowerCase();

        // 基于关键词的特征编码
        String[][] keywordGroups = {
            {"机油", "润滑油", "oil"}, {"轮胎", "胎压", "tire"}, {"刹车", "制动", "brake"},
            {"电池", "电瓶", "battery"}, {"空调", "冷气", "ac"}, {"变速箱", "变速", "transmission"},
            {"火花塞", "点火"}, {"滤芯", "过滤", "filter"}, {"冷却", "防冻", "coolant"},
            {"转向", "方向盘"}, {"悬挂", "避震"}, {"正时", "皮带"},
            {"丰田", "卡罗拉", "corolla"}, {"本田", "飞度", "honda"}, {"大众", "volkswagen"},
            {"宝马", "bmw"}, {"奔驰", "benz"}, {"奥迪", "audi"}, {"特斯拉", "tesla"},
            {"比亚迪", "byd"}, {"理想"}, {"蔚来"}, {"红旗"}, {"别克"},
            {"轿车"}, {"suv"}, {"mpv"}, {"新能源", "纯电"},
            {"常规", "保养"}, {"大修", "维修"}, {"检查", "检测"},
            {"5000", "10000", "15000", "20000", "30000", "50000"}
        };

        // 每组关键词映射到不同的向量区域
        for (int g = 0; g < keywordGroups.length; g++) {
            for (String kw : keywordGroups[g]) {
                if (lower.contains(kw)) {
                    int baseIdx = (g * 50) % EmbeddingService.EMBEDDING_DIM;
                    for (int j = 0; j < 50; j++) {
                        int idx = (baseIdx + j) % EmbeddingService.EMBEDDING_DIM;
                        vector[idx] += 1.0f / (j + 1);
                    }
                }
            }
        }

        // 数字特征（里程、费用等）
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\d+").matcher(text);
        int numIdx = 0;
        while (matcher.find() && numIdx < 20) {
            int val = Integer.parseInt(matcher.group());
            int base = (1400 + numIdx * 8) % EmbeddingService.EMBEDDING_DIM;
            for (int j = 0; j < 8; j++) {
                vector[(base + j) % EmbeddingService.EMBEDDING_DIM] += val / 10000.0f;
            }
            numIdx++;
        }

        // 归一化
        double norm = 0;
        for (float v : vector) norm += v * v;
        norm = Math.sqrt(norm);
        if (norm > 0) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] /= norm;
            }
        }

        return vector;
    }
}
