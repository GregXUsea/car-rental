package com.carrental.service;

import com.carrental.dto.AIRecommendResult;
import com.carrental.entity.Car;
import com.carrental.entity.MaintenanceRecord;
import com.carrental.mapper.MaintenanceRecordMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class AIService {

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.base-url}")
    private String baseUrl;

    @Value("${openai.model}")
    private String model;

    @Autowired
    private CarService carService;

    @Autowired
    private MaintenanceRecordMapper maintenanceRecordMapper;

    @Autowired
    private RAGService ragService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    public AIRecommendResult recommendCars(String userRequirement) {
        List<Car> availableCars = carService.listRentable();

        // 先用本地关键词匹配
        AIRecommendResult localResult = fallbackRecommend(userRequirement, availableCars);

        // 如果 API key 未配置或无效，直接返回本地结果
        if (apiKey == null || apiKey.equals("sk-your-api-key-here") || apiKey.isBlank()) {
            return localResult;
        }

        StringBuilder carListStr = new StringBuilder();
        for (Car car : availableCars) {
            carListStr.append(String.format(
                    "ID:%d, %s %s, %s, %d座, %.0f元/天, %s, 用途:%s, 里程%dkm, 保养%d次, %s\n",
                    car.getId(), car.getBrand(), car.getModel(), car.getColor(),
                    car.getSeats(), car.getPricePerDay(), car.getCategory(),
                    car.getUsageType() != null ? car.getUsageType() : "通用",
                    car.getMileage(), car.getMaintainCount() != null ? car.getMaintainCount() : 0,
                    car.getDescription()));
        }

        String systemPrompt = "你是一个专业的汽车租赁顾问AI助手。用户会描述他们的用车需求，你需要从可用车辆列表中推荐最匹配的车型。\n\n" +
                "【核心规则】\n" +
                "1. 必须从提供的车辆列表中选择，不要推荐列表中没有的车型\n" +
                "2. 根据用户预算、人数、用途等筛选最合适的车辆\n" +
                "3. 商务接待：推荐用途包含「商务」的车辆\n" +
                "4. 婚庆用车：推荐用途包含「婚庆」的车辆\n" +
                "5. 家庭出游：推荐用途包含「家庭」的车辆\n" +
                "6. 日常通勤：推荐用途包含「通勤」的车辆\n\n" +
                "【输出格式 - 必须严格遵守】\n" +
                "只返回JSON，不要任何其他文字、标题或说明。格式：\n" +
                "{\"summary\":\"一句话总结推荐理由\",\"recommendations\":[{\"carId\":数字ID,\"reason\":\"推荐理由（30字内）\",\"matchScore\":\"95%\"}]}\n\n" +
                "要求：\n" +
                "- 最多推荐3辆车，按匹配度从高到低\n" +
                "- carId必须是车辆列表中的真实ID\n" +
                "- matchScore是百分比数字\n" +
                "- 只返回纯JSON，不要markdown代码块";

        String userPrompt = String.format("我的需求：%s\n\n可用车辆列表：\n%s", userRequirement, carListStr);

        try {
            String response = callOpenAI(systemPrompt, userPrompt);
            return parseRecommendResult(response, availableCars);
        } catch (Exception e) {
            return localResult;
        }
    }

    /**
     * 通用AI对话（支持租车推荐 + 普通问答 + 上下文记忆）
     */
    public Map<String, Object> chat(String userMessage, List<Map<String, String>> history) {
        Map<String, Object> result = new HashMap<>();

        // 判断是否为明确的租车需求（需要推荐车辆）
        boolean isRentalNeed = isRentalNeedQuestion(userMessage);

        if (isRentalNeed) {
            // 明确租车需求：调用推荐逻辑
            AIRecommendResult recommendResult = recommendCars(userMessage);
            result.put("type", "recommend");
            result.put("reply", recommendResult.getSummary());
            result.put("recommendations", recommendResult.getRecommendations());
        } else {
            // 其他所有问题（包括汽车知识、通用问答）：调用AI回答
            if (apiKey == null || apiKey.equals("sk-your-api-key-here") || apiKey.isBlank()) {
                result.put("type", "text");
                result.put("reply", generateLocalReply(userMessage));
            } else {
                try {
                    // 获取可用车辆列表，传给AI
                    List<Car> availableCars = carService.listRentable();
                    StringBuilder carListStr = new StringBuilder();
                    for (Car car : availableCars) {
                        carListStr.append(String.format("ID:%d, %s %s, %s, %d座, ¥%.0f/天, %s\n",
                                car.getId(), car.getBrand(), car.getModel(), car.getColor(),
                                car.getSeats(), car.getPricePerDay(),
                                car.getUsageType() != null ? car.getUsageType() : "通用"));
                    }

                    String systemPrompt = "你是「御途租车」的AI智能助手，名叫「途途」。\n\n" +
                            "## 可用车辆列表\n" + carListStr + "\n\n" +
                            "## 回答规范\n" +
                            "- 用中文回答，语言自然流畅\n" +
                            "- 回答详细充实，给出具体建议\n" +
                            "- 结构清晰，使用分点列表\n" +
                            "- 记住对话上下文\n" +
                            "- 即使问题与租车无关，也要友好回答\n\n" +
                            "## 车辆推荐规则\n" +
                            "当用户询问与车、出行、旅游、通勤等相关问题时，必须推荐车辆。\n" +
                            "从上面的可用车辆列表中选择合适的车辆推荐。\n\n" +
                            "## 车辆链接格式（重要！）\n" +
                            "推荐车辆时，必须在回答中添加可点击链接，格式严格如下：\n" +
                            "[查看XX车型详情](/car/车辆ID)\n" +
                            "例如：[查看宝马5系详情](/car/13)\n\n" +
                            "注意：车辆ID必须是上面列表中的真实ID！";

                    String response = callOpenAIWithHistory(systemPrompt, userMessage, history);
                    result.put("type", "text");
                    result.put("reply", response);
                } catch (Exception e) {
                    result.put("type", "text");
                    result.put("reply", generateLocalReply(userMessage));
                }
            }
        }

        return result;
    }

    /**
     * 判断是否为明确的租车需求（需要推荐车辆）
     * 更智能的判断：理解多种表达方式
     */
    private boolean isRentalNeedQuestion(String message) {
        String lower = message.toLowerCase();

        // 排除明显非租车的推荐请求（如：推荐书、推荐电影、推荐游戏、推荐歌曲等）
        String[] nonCarRecommendPatterns = {"小说", "电影", "电视剧", "歌曲", "音乐", "书",
                "游戏", "动漫", "综艺", "节目", "app", "软件", "美食", "餐厅", "旅游景点",
                "酒店", "机票", "火车票", "快递", "外卖", "新闻", "天气", "笑话", "故事",
                "音乐", "歌曲", "歌手", "明星", "演员", "导演", "作家", "诗人",
                "股票", "基金", "理财", "投资", "房价", "房价", "经济", "政治",
                "历史", "文化", "教育", "科技", "医学", "法律", "哲学", "宗教",
                "剑来", "斗破苍穹", "完美世界", "遮天", "凡人修仙传"};
        for (String pattern : nonCarRecommendPatterns) {
            if (lower.contains(pattern)) return false;
        }

        // 直接租车需求（必须包含"租"字）
        if (lower.contains("租")) {
            // "想租"、"需要租"、"租什么车"等明确意图
            String[] directRental = {"想租", "需要租", "租什么", "租哪", "怎么租", "何时租", "租车",
                    "租赁", "用车", "包车", "自驾"};
            for (String keyword : directRental) {
                if (lower.contains(keyword)) return true;
            }
            // "租" + 场景词
            String[] sceneKeywords = {"商务", "婚庆", "婚礼", "家庭", "出游", "旅游", "通勤", "代步",
                    "SUV", "轿车", "MPV", "新能源", "豪华", "经济", "便宜", "上班", "过年", "暑假",
                    "周末", "节假日", "长途", "短途", "机场", "高铁"};
            for (String scene : sceneKeywords) {
                if (lower.contains(scene)) return true;
            }
        }

        // 带"车"字的推荐请求
        if (lower.contains("车")) {
            String[] carRecommend = {"推荐", "有没有", "选哪", "哪辆", "什么车好", "哪款", "帮我选",
                    "适合", "合适", "比较好", "不错", "性价比", "便宜", "划算"};
            for (String keyword : carRecommend) {
                if (lower.contains(keyword)) return true;
            }
        }

        // 询问车型、价格、座位数等（隐含租车意图）
        String[] implicitRental = {"五座", "七座", "七座车", "五座车", "几个座", "多少座",
                "日租", "天租", "每小时", "押金", "租金", "违章", "保险"};
        for (String keyword : implicitRental) {
            if (lower.contains(keyword)) return true;
        }

        return false;
    }

    /**
     * 本地智能回复（未配置API时使用）
     */
    private String generateLocalReply(String message) {
        String lower = message.toLowerCase();

        // 问候
        if (lower.contains("你好") || lower.contains("hi") || lower.contains("hello") || lower.contains("在吗")) {
            return "你好！我是御途租车的AI助手「途途」，很高兴为您服务！😊\n\n" +
                    "我可以帮您：\n" +
                    "🚗 推荐合适的车型\n" +
                    "💰 查询租车价格\n" +
                    "📋 了解租车流程\n" +
                    "🔧 车辆保养建议\n" +
                    "💬 回答各种问题\n\n" +
                    "请问有什么可以帮您的？";
        }

        // 价格相关
        if (lower.contains("价格") || lower.contains("多少钱") || lower.contains("费用")) {
            return "我们的车辆日租金从 ¥118 到 ¥888 不等，具体取决于车型：\n\n" +
                    "• 经济型（飞度、逸动等）：¥118-158/天\n" +
                    "• 舒适型（卡罗拉、雅阁等）：¥158-238/天\n" +
                    "• 豪华型（宝马5系、奔驰E级等）：¥438-888/天\n" +
                    "• 新能源（理想L7、蔚来ES6等）：¥248-398/天\n\n" +
                    "🎉 新用户首单可享5折优惠，最高减200元！\n\n" +
                    "如需了解具体车型价格，请告诉我您的需求。";
        }

        // 流程相关
        if (lower.contains("流程") || lower.contains("怎么租") || lower.contains("如何") || lower.contains("步骤")) {
            return "租车流程非常简单：\n\n" +
                    "1️⃣ 注册/登录账号\n" +
                    "2️⃣ 浏览车型，选择心仪的车辆\n" +
                    "3️⃣ 选择租车时间（立即租/预约）\n" +
                    "4️⃣ 支付押金\n" +
                    "5️⃣ 到店取车，开始旅程\n" +
                    "6️⃣ 用完后归还车辆，结算费用\n\n" +
                    "🎉 新用户注册即享200元优惠券，首次租车5折起！";
        }

        // 保养知识
        if (lower.contains("保养") || lower.contains("维护") || lower.contains("多久保养")) {
            return "车辆保养知识：\n\n" +
                    "📌 常规保养周期：\n" +
                    "• 每5000-10000km或每6个月进行常规保养\n" +
                    "• 每30000-50000km进行大保养\n\n" +
                    "📌 保养项目：\n" +
                    "• 更换机油、机滤\n" +
                    "• 检查刹车片、轮胎\n" +
                    "• 检查各种液位\n" +
                    "• 新能源车检测电池健康度\n\n" +
                    "📌 注意事项：\n" +
                    "• 长途行驶后建议检查车况\n" +
                    "• 异常响声及时检查\n" +
                    "• 定期检查轮胎气压";
        }

        // 汽车知识
        if (lower.contains("汽车") || lower.contains("轿车") || lower.contains("suv") || lower.contains("新能源")) {
            return "汽车知识科普：\n\n" +
                    "🚗 车型分类：\n" +
                    "• 轿车：舒适省油，适合日常通勤\n" +
                    "• SUV：空间大、通过性强，适合家庭出游\n" +
                    "• MPV：座位多、空间超大，适合商务接待\n" +
                    "• 新能源：用车成本低，环保节能\n\n" +
                    "💡 选车建议：\n" +
                    "• 预算有限选经济型轿车\n" +
                    "• 家庭出行选7座SUV或MPV\n" +
                    "• 商务接待选豪华品牌\n" +
                    "• 城市通勤选新能源\n\n" +
                    "想了解更多具体车型，可以告诉我您的需求！";
        }

        // 客服联系
        if (lower.contains("客服") || lower.contains("电话") || lower.contains("联系") || lower.contains("投诉")) {
            return "您可以通过以下方式联系我们：\n\n" +
                    "📞 客服热线：400-888-8888\n" +
                    "📱 微信客服：YUTU_CAR\n" +
                    "📧 邮箱：service@yutu.com\n" +
                    "🕐 服务时间：8:00-22:00\n\n" +
                    "如有紧急问题，可直接拨打客服热线。";
        }

        // 天气/生活
        if (lower.contains("天气") || lower.contains("今天")) {
            return "我是AI助手，暂时无法查询实时天气哦～\n\n" +
                    "不过我可以帮您：\n" +
                    "🚗 推荐适合出行的车型\n" +
                    "📋 了解租车流程\n" +
                    "💰 查询租车价格\n\n" +
                    "请问还有什么可以帮您的？";
        }

        // 违章相关
        if (lower.contains("违章") || lower.contains("罚款") || lower.contains("扣分")) {
            return "关于租车违章的说明：\n\n" +
                    "📌 违章处理规则：\n" +
                    "• 租车期间产生的违章由承租人承担\n" +
                    "• 违章罚款 + 扣分均由承租人负责\n" +
                    "• 我们会协助处理违章查询\n\n" +
                    "📌 违章处理流程：\n" +
                    "1. 还车后15个工作日内查询违章\n" +
                    "2. 如有违章，我们会通知您\n" +
                    "3. 您可通过交管12123或到交警大队处理\n\n" +
                    "📌 注意事项：\n" +
                    "• 请遵守交通规则，安全驾驶\n" +
                    "• 如对违章有异议，可联系客服协助处理";
        }

        // 保险相关
        if (lower.contains("保险") || lower.contains("理赔") || lower.contains("事故")) {
            return "关于租车保险的说明：\n\n" +
                    "📌 基础保险（已包含在租金中）：\n" +
                    "• 交强险（法定必购）\n" +
                    "• 车损险（1500元以下免赔）\n\n" +
                    "📌 可选增值服务：\n" +
                    "• 不计免赔：¥30/天，1500元以下免赔转为0\n" +
                    "• 车身划痕险：¥20/天\n" +
                    "• 轮胎险：¥15/天\n\n" +
                    "📌 事故处理：\n" +
                    "1. 确保人员安全，及时报警\n" +
                    "2. 拍照保留现场证据\n" +
                    "3. 联系客服报备\n" +
                    "4. 按指引进行理赔";
        }

        // 驾照相关
        if (lower.contains("驾照") || lower.contains("驾驶证") || lower.contains("驾龄")) {
            return "关于租车驾照要求：\n\n" +
                    "📌 基本要求：\n" +
                    "• 持有有效期内的中国驾照\n" +
                    "• 驾照状态正常（未吊销/暂扣）\n" +
                    "• 实习期可租小型汽车（C1/C2）\n\n" +
                    "📌 不同车型要求：\n" +
                    "• 小型车（轿车/SUV）：C1/C2即可\n" +
                    "• 中型车（MPV 7座以上）：B1及以上\n" +
                    "• 豪华车：建议驾龄1年以上\n\n" +
                    "📌 取车时需携带：\n" +
                    "• 本人有效驾照\n" +
                    "• 身份证原件";
        }

        // 还车相关
        if (lower.contains("还车") || lower.contains("归还") || lower.contains("退车")) {
            return "还车流程说明：\n\n" +
                    "📌 还车时间：\n" +
                    "• 按订单约定时间归还\n" +
                    "• 提前还车可退还差额\n" +
                    "• 超时还车可能产生额外费用\n\n" +
                    "📌 还车地点：\n" +
                    "• 原取车门店（默认）\n" +
                    "• 支持异地还车（需提前申请，可能产生费用）\n\n" +
                    "📌 还车流程：\n" +
                    "1. 联系客服确认还车\n" +
                    "2. 到店进行车辆检查\n" +
                    "3. 确认里程和油量\n" +
                    "4. 结算费用，退还押金\n\n" +
                    "📌 注意事项：\n" +
                    "• 还车前加满油\n" +
                    "• 清理车内个人物品\n" +
                    "• 检查有无新增损伤";
        }

        // 优惠券相关
        if (lower.contains("优惠券") || lower.contains("优惠") || lower.contains("折扣") || lower.contains("打折")) {
            return "当前优惠活动：\n\n" +
                    "🎉 新用户专享：\n" +
                    "• 注册30天内首单5折\n" +
                    "• 最高优惠200元\n" +
                    "• 无门槛使用\n\n" +
                    "📌 优惠规则：\n" +
                    "• 每用户限享一次\n" +
                    "• 不可与其他优惠叠加\n" +
                    "• 仅限首笔订单使用\n\n" +
                    "📌 如何使用：\n" +
                    "• 符合条件的用户在支付时自动享受\n" +
                    "• 无需手动领取\n\n" +
                    "更多优惠活动请关注首页公告！";
        }

        // 支付方式
        if (lower.contains("支付") || lower.contains("付款") || lower.contains("押金") || lower.contains("退款")) {
            return "支付相关说明：\n\n" +
                    "📌 支付方式：\n" +
                    "• 银行卡支付（模拟）\n" +
                    "• 支持借记卡/信用卡\n\n" +
                    "📌 押金说明：\n" +
                    "• 押金金额由车型决定（1500-15000元）\n" +
                    "• 还车后1-3个工作日退还\n" +
                    "• 如有违章/损伤，扣除相应费用后退还\n\n" +
                    "📌 退款规则：\n" +
                    "• 提前还车：退还差额\n" +
                    "• 取消订单：按取消时间退还\n" +
                    "• 退款原路返回";
        }

        // 默认回复
        return "感谢您的提问！我是御途租车的AI助手「途途」，我可以：\n\n" +
                "🚗 根据需求推荐车型\n" +
                "💰 查询租车价格和优惠\n" +
                "📋 解答租车流程问题\n" +
                "🔧 提供车辆保养建议\n" +
                "💬 回答各种汽车相关问题\n" +
                "📝 回答通用知识问题\n\n" +
                "您可以试着问我：\n" +
                "• 「推荐一辆商务用车」\n" +
                "• 「家庭出游租什么车好？」\n" +
                "• 「SUV和轿车哪个好？」\n" +
                "• 「新能源车有哪些优点？」\n" +
                "• 「还车流程是什么？」\n" +
                "• 「保险怎么买？」";
    }

    /**
     * 快速预测（纯本地计算，不调OpenAI，用于初始加载）
     */
    public Map<String, Object> getQuickPrediction(Long carId) {
        Car car = carService.getById(carId);
        if (car == null) throw new RuntimeException("车辆不存在");

        List<MaintenanceRecord> records = maintenanceRecordMapper.selectList(
                new LambdaQueryWrapper<MaintenanceRecord>()
                        .eq(MaintenanceRecord::getCarId, carId)
                        .orderByDesc(MaintenanceRecord::getMaintenanceDate));

        int mileageSinceLastMaintain = 0;
        long daysSinceLastMaintain = 0;
        if (car.getLastMaintainDate() != null) {
            daysSinceLastMaintain = java.time.temporal.ChronoUnit.DAYS.between(
                    car.getLastMaintainDate(), java.time.LocalDate.now());
            if (!records.isEmpty()) {
                mileageSinceLastMaintain = car.getMileage() - records.get(0).getMileageAtMaintenance();
            }
        }

        List<RAGService.RetrievalResult> similarCases = ragService.retrieveSimilarCases(carId, 3);
        return buildFallbackPrediction(car, records, mileageSinceLastMaintain, daysSinceLastMaintain, similarCases);
    }

    public Map<String, Object> getMaintenancePrediction(Long carId) {
        Car car = carService.getById(carId);
        if (car == null) {
            throw new RuntimeException("车辆不存在");
        }

        List<MaintenanceRecord> records = maintenanceRecordMapper.selectList(
                new LambdaQueryWrapper<MaintenanceRecord>()
                        .eq(MaintenanceRecord::getCarId, carId)
                        .orderByDesc(MaintenanceRecord::getMaintenanceDate));

        StringBuilder recordStr = new StringBuilder();
        for (MaintenanceRecord r : records) {
            recordStr.append(String.format("日期:%s, 里程:%dkm, 类型:%s, 描述:%s, 费用:%.0f元\n",
                    r.getMaintenanceDate(), r.getMileageAtMaintenance(),
                    r.getMaintenanceType(), r.getDescription(), r.getCost()));
        }

        // ====== RAG 相似案例检索 ======
        List<RAGService.RetrievalResult> similarCases = ragService.retrieveSimilarCases(carId, 3);
        StringBuilder similarCaseStr = new StringBuilder();
        if (!similarCases.isEmpty()) {
            similarCaseStr.append("\n\n【相似车辆维护案例（RAG检索结果）】\n");
            for (RAGService.RetrievalResult result : similarCases) {
                RAGService.KnowledgeEntry entry = result.getEntry();
                similarCaseStr.append(String.format(
                        "- %s：%s保养，%dkm时进行，\"%s\"，费用%.0f元，相似度%.1f%%\n",
                        entry.getCarName(), entry.getMaintenanceType(),
                        entry.getMileage(), entry.getDescription(),
                        entry.getCost(), result.getSimilarity() * 100));
            }
        }

        // 计算距上次保养的里程增量和时间
        int mileageSinceLastMaintain = 0;
        long daysSinceLastMaintain = 0;
        if (car.getLastMaintainDate() != null) {
            daysSinceLastMaintain = java.time.temporal.ChronoUnit.DAYS.between(
                    car.getLastMaintainDate(), java.time.LocalDate.now());
            // 查找上次保养时的里程
            if (!records.isEmpty()) {
                int lastMaintainMileage = records.get(0).getMileageAtMaintenance();
                mileageSinceLastMaintain = car.getMileage() - lastMaintainMileage;
            }
        }

        String systemPrompt = "你是一个专业的汽车维护顾问。根据车辆信息、历史维护记录、里程数据以及RAG检索到的相似车辆维护案例，预测下次维护时间和建议。\n\n" +
                "请综合考虑：\n" +
                "1. 车辆自身的保养历史和当前里程\n" +
                "2. 距上次保养已行驶的里程数和天数\n" +
                "3. 相似车辆在类似里程/时间的维护经验\n" +
                "4. 不同品牌/车型的保养特点\n\n" +
                "里程参考阈值：\n" +
                "- 常规保养：每5000-10000km或每6个月\n" +
                "- 大保养：每30000-50000km或每2年\n" +
                "- 新能源车：检查周期可适当延长\n\n" +
                "riskScore评分标准（0-100）：\n" +
                "- 0-24分=低风险，25-49分=中风险，50-100分=高风险\n" +
                "- 综合里程、时间、保养频率、维修历史等因素打分\n\n" +
                "用中文回答，返回JSON格式：\n" +
                "{\"nextMaintenanceDate\":\"预计日期\",\"nextMaintenanceType\":\"保养类型\",\"suggestions\":[\"建议1\",\"建议2\"],\"riskLevel\":\"低/中/高\",\"riskScore\":数字分数,\"similarCasesSummary\":\"相似案例总结\",\"mileageSinceLastMaintain\":距上次保养里程,\"daysSinceLastMaintain\":距上次保养天数}\n" +
                "只返回JSON。";

        String userPrompt = String.format(
                "车辆：%s %s, 当前里程:%dkm, 上次保养:%s, 距上次保养已行驶%dkm, 距上次保养已%d天\n\n历史维护记录：\n%s%s",
                car.getBrand(), car.getModel(), car.getMileage(),
                car.getLastMaintainDate(), mileageSinceLastMaintain, daysSinceLastMaintain,
                recordStr, similarCaseStr);

        try {
            String response = callOpenAI(systemPrompt, userPrompt);
            Map<String, Object> result = objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {});
            // 附带 RAG 检索到的相似案例信息
            if (!similarCases.isEmpty()) {
                List<Map<String, Object>> caseList = new ArrayList<>();
                for (RAGService.RetrievalResult r : similarCases) {
                    Map<String, Object> caseMap = new HashMap<>();
                    caseMap.put("carName", r.getEntry().getCarName());
                    caseMap.put("maintenanceType", r.getEntry().getMaintenanceType());
                    caseMap.put("mileage", r.getEntry().getMileage());
                    caseMap.put("description", r.getEntry().getDescription());
                    caseMap.put("date", r.getEntry().getDate());
                    caseMap.put("cost", r.getEntry().getCost());
                    caseMap.put("similarity", Math.round(r.getSimilarity() * 100));
                    caseList.add(caseMap);
                }
                result.put("similarCases", caseList);
            }
            // 如果AI没返回riskScore，根据riskLevel补一个默认分数
            if (result.get("riskScore") == null) {
                String level = String.valueOf(result.getOrDefault("riskLevel", "中"));
                int score = level.contains("高") ? 65 : level.contains("低") ? 15 : 35;
                result.put("riskScore", score);
            }
            return result;
        } catch (Exception e) {
            return buildFallbackPrediction(car, records, mileageSinceLastMaintain, daysSinceLastMaintain, similarCases);
        }
    }

    /**
     * 基于数据的降级预测（无AI时使用）
     */
    private Map<String, Object> buildFallbackPrediction(Car car, List<MaintenanceRecord> records,
            int mileageSinceLastMaintain, long daysSinceLastMaintain,
            List<RAGService.RetrievalResult> similarCases) {

        Map<String, Object> fallback = new HashMap<>();
        int mileage = car.getMileage() != null ? car.getMileage() : 0;

        // === 风险评估 ===
        int riskScore = 10; // 基础分
        List<String> suggestions = new ArrayList<>();

        // 里程因素（权重最高）
        if (mileage > 80000) {
            riskScore += 45;
            suggestions.add("⚠️ 总里程超过8万km，建议全面检查底盘、悬挂系统");
            suggestions.add("建议检查变速箱油和正时皮带/链条状态");
        } else if (mileage > 60000) {
            riskScore += 35;
            suggestions.add("⚠️ 总里程超过6万km，建议检查刹车片、刹车盘磨损情况");
            suggestions.add("建议检查轮胎花纹深度和悬挂系统");
        } else if (mileage > 40000) {
            riskScore += 25;
            suggestions.add("里程超过4万km，建议检查刹车系统和轮胎状况");
            suggestions.add("建议关注发动机异响和变速箱换挡平顺性");
        } else if (mileage > 20000) {
            riskScore += 10;
            suggestions.add("里程进入中期，建议定期检查各项液位");
        }

        // 距上次保养里程因素
        if (mileageSinceLastMaintain > 8000) {
            riskScore += 30;
            suggestions.add("⚠️ 距上次保养已超过8000km，强烈建议尽快保养");
        } else if (mileageSinceLastMaintain > 5000) {
            riskScore += 20;
            suggestions.add("距上次保养接近保养周期，建议近期安排保养");
        } else if (mileageSinceLastMaintain > 3000) {
            riskScore += 10;
            suggestions.add("建议关注机油状态和各项液位");
        }

        // 距上次保养时间因素
        if (daysSinceLastMaintain > 365) {
            riskScore += 25;
            suggestions.add("⚠️ 超过1年未保养，建议立即进行全面检查");
        } else if (daysSinceLastMaintain > 180) {
            riskScore += 15;
            suggestions.add("超过半年未保养，建议尽快安排常规保养");
        } else if (daysSinceLastMaintain > 90) {
            riskScore += 5;
            suggestions.add("建议近期安排常规保养");
        }

        // 维修频率因素（频繁维修 = 车况差）
        long repairCount = records.stream()
                .filter(r -> r.getMaintenanceType() != null &&
                        (r.getMaintenanceType().contains("维修") || r.getMaintenanceType().contains("故障") ||
                         r.getMaintenanceType().contains("更换")))
                .count();
        if (repairCount >= 3) {
            riskScore += 30;
            suggestions.add("⚠️ 历史维修/故障次数较多（" + repairCount + "次），建议重点关注核心部件状况");
        } else if (repairCount >= 2) {
            riskScore += 20;
            suggestions.add("有多次维修历史，建议定期检查相关部件");
        } else if (repairCount >= 1) {
            riskScore += 10;
            suggestions.add("有维修历史，建议关注相关部件状况");
        }

        // 保养频率因素（保养次数少但里程高 = 风险）
        if (records.size() <= 1 && mileage > 20000) {
            riskScore += 15;
            suggestions.add("保养记录较少但里程较高，建议增加保养频率");
        }

        // 车辆状态因素
        if (car.getStatus() != null && car.getStatus() == 3) {
            riskScore += 20;
            suggestions.add("车辆当前处于维护状态，需完成检修后方可出租");
        }

        // 新能源车特殊建议
        if ("新能源".equals(car.getCategory())) {
            suggestions.add("新能源车建议检查电池健康度和充电效率");
            if (mileage > 30000) {
                suggestions.add("建议检测电池衰减程度，评估续航表现");
            }
        }

        // 通用建议
        if (suggestions.isEmpty()) {
            suggestions.add("车况良好，建议按期进行常规保养");
            suggestions.add("定期检查轮胎气压和刹车系统");
        }

        // === 风险等级判定 ===
        String riskLevel;
        if (riskScore >= 50) {
            riskLevel = "高";
        } else if (riskScore >= 25) {
            riskLevel = "中";
        } else {
            riskLevel = "低";
        }

        // === 预测下次保养日期 ===
        String nextDate;
        String nextType;
        if (mileageSinceLastMaintain > 10000 || daysSinceLastMaintain > 365) {
            nextDate = "建议立即保养";
            nextType = mileage > 30000 ? "大保养" : "常规保养";
        } else if (mileageSinceLastMaintain > 5000 || daysSinceLastMaintain > 180) {
            // 预计30天内需要保养
            nextDate = java.time.LocalDate.now().plusDays(30).toString();
            nextType = "常规保养";
        } else {
            // 预计按保养周期推算
            int daysToNext = (int) Math.max(30, 180 - daysSinceLastMaintain);
            int kmToNext = Math.max(500, 5000 - mileageSinceLastMaintain);
            nextDate = java.time.LocalDate.now().plusDays(Math.min(daysToNext, 90)).toString();
            nextType = "常规保养";
            suggestions.add(String.format("预计还需行驶约%dkm达到保养周期", kmToNext));
        }

        // === 寿命评估（高里程车） ===
        if (mileage > 40000) {
            int estimatedLifespan = 150000; // 一般家用车15万km寿命
            int remainingPercent = Math.max(0, (int) ((1.0 - (double) mileage / estimatedLifespan) * 100));
            suggestions.add(String.format("📊 车辆寿命评估：已行驶%dkm，预估剩余寿命约%d%%", mileage, remainingPercent));
            if (remainingPercent < 40) {
                if (!riskLevel.equals("高")) riskLevel = "高"; // 寿命不足40%，强制高风险
                suggestions.add("⚠️ 车辆接近使用寿命上限，建议评估是否继续投入维护成本");
            } else if (remainingPercent < 60) {
                if (riskLevel.equals("低")) riskLevel = "中"; // 寿命不足60%，至少中风险
                suggestions.add("车辆已过使用寿命中期，建议加强保养频率");
            }
        }

        fallback.put("nextMaintenanceDate", nextDate);
        fallback.put("nextMaintenanceType", nextType);
        fallback.put("suggestions", suggestions);
        fallback.put("riskLevel", riskLevel);
        fallback.put("mileageSinceLastMaintain", mileageSinceLastMaintain);
        fallback.put("daysSinceLastMaintain", daysSinceLastMaintain);
        fallback.put("riskScore", riskScore);

        if (!similarCases.isEmpty()) {
            StringBuilder summary = new StringBuilder();
            summary.append("基于").append(similarCases.size()).append("个相似案例：");
            for (RAGService.RetrievalResult r : similarCases) {
                summary.append(r.getEntry().getCarName()).append("(")
                        .append(r.getEntry().getMileage()).append("km,")
                        .append(r.getEntry().getMaintenanceType()).append(") ");
            }
            fallback.put("similarCasesSummary", summary.toString());

            List<Map<String, Object>> caseList = new ArrayList<>();
            for (RAGService.RetrievalResult r : similarCases) {
                Map<String, Object> caseMap = new HashMap<>();
                caseMap.put("carName", r.getEntry().getCarName());
                caseMap.put("maintenanceType", r.getEntry().getMaintenanceType());
                caseMap.put("mileage", r.getEntry().getMileage());
                caseMap.put("description", r.getEntry().getDescription());
                caseMap.put("date", r.getEntry().getDate());
                caseMap.put("cost", r.getEntry().getCost());
                caseMap.put("similarity", Math.round(r.getSimilarity() * 100));
                caseList.add(caseMap);
            }
            fallback.put("similarCases", caseList);
        }

        return fallback;
    }

    private String callOpenAI(String systemPrompt, String userPrompt) throws IOException {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)));
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 1500);

        String json = objectMapper.writeValueAsString(requestBody);

        Request request = new Request.Builder()
                .url(baseUrl + "/chat/completions")
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(json, MediaType.parse("application/json")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("API call failed: " + response.code());
            }
            String body = response.body().string();
            JsonNode root = objectMapper.readTree(body);
            return root.path("choices").get(0).path("message").path("content").asText();
        }
    }

    /**
     * 调用OpenAI兼容API（带对话历史）
     */
    private String callOpenAIWithHistory(String systemPrompt, String userMessage, List<Map<String, String>> history) throws IOException {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));

        // 添加历史对话（最多保留最近10轮）
        if (history != null) {
            int start = Math.max(0, history.size() - 20); // 最多20条消息（10轮对话）
            for (int i = start; i < history.size(); i++) {
                Map<String, String> msg = history.get(i);
                String role = msg.get("role");
                String content = msg.get("content");
                if (role != null && content != null && (role.equals("user") || role.equals("assistant"))) {
                    messages.add(Map.of("role", role, "content", content));
                }
            }
        }

        messages.add(Map.of("role", "user", "content", userMessage));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 1500);

        String json = objectMapper.writeValueAsString(requestBody);

        Request request = new Request.Builder()
                .url(baseUrl + "/chat/completions")
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(json, MediaType.parse("application/json")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("API call failed: " + response.code());
            }
            String body = response.body().string();
            JsonNode root = objectMapper.readTree(body);
            return root.path("choices").get(0).path("message").path("content").asText();
        }
    }

    private AIRecommendResult parseRecommendResult(String response, List<Car> availableCars) {
        try {
            // 清理可能的markdown包裹
            String json = response.trim();
            if (json.startsWith("```")) {
                json = json.replaceAll("```json?", "").replaceAll("```", "").trim();
            }

            // 尝试找到JSON对象
            int start = json.indexOf('{');
            int end = json.lastIndexOf('}');
            if (start >= 0 && end > start) {
                json = json.substring(start, end + 1);
            }

            JsonNode root = objectMapper.readTree(json);
            AIRecommendResult result = new AIRecommendResult();
            result.setSummary(root.path("summary").asText("为您推荐以下车型"));

            Map<Long, Car> carMap = availableCars.stream()
                    .collect(Collectors.toMap(Car::getId, c -> c));

            List<AIRecommendResult.RecommendItem> items = new ArrayList<>();
            JsonNode recs = root.path("recommendations");
            if (recs.isArray()) {
                for (JsonNode rec : recs) {
                    AIRecommendResult.RecommendItem item = new AIRecommendResult.RecommendItem();
                    Long carId = rec.path("carId").asLong();
                    item.setCar(carMap.get(carId));
                    item.setReason(rec.path("reason").asText("符合您的需求"));
                    item.setMatchScore(rec.path("matchScore").asText("85%"));
                    if (item.getCar() != null) {
                        items.add(item);
                    }
                }
            }
            result.setRecommendations(items);
            return result;
        } catch (Exception e) {
            // 解析失败，使用本地推荐
            return fallbackRecommend("", availableCars);
        }
    }

    private AIRecommendResult fallbackRecommend(String requirement, List<Car> cars) {
        AIRecommendResult result = new AIRecommendResult();

        String req = requirement.toLowerCase();
        List<Car> scored = new ArrayList<>(cars);

        // 根据关键词给车辆打分排序
        scored.sort((a, b) -> {
            int scoreA = calcMatchScore(a, req);
            int scoreB = calcMatchScore(b, req);
            return scoreB - scoreA;
        });

        List<AIRecommendResult.RecommendItem> items = new ArrayList<>();
        int count = Math.min(3, scored.size());
        for (int i = 0; i < count; i++) {
            Car car = scored.get(i);
            AIRecommendResult.RecommendItem item = new AIRecommendResult.RecommendItem();
            item.setCar(car);
            item.setReason(buildReason(car, req));
            item.setMatchScore(calcMatchScore(car, req) + "%");
            items.add(item);
        }

        // 生成更友好的摘要
        StringBuilder summary = new StringBuilder();
        summary.append(String.format("根据您的需求，从 %d 辆可用车辆中精选了 %d 辆最合适的车型", cars.size(), items.size()));
        if (!items.isEmpty()) {
            Car topCar = items.get(0).getCar();
            summary.append("，首推「").append(topCar.getBrand()).append(" ").append(topCar.getModel()).append("」");
            if (topCar.getPricePerDay().doubleValue() < 200) {
                summary.append("，日租仅").append(topCar.getPricePerDay()).append("元，性价比超高！");
            } else {
                summary.append("，品质出众！");
            }
        }
        result.setSummary(summary.toString());
        result.setRecommendations(items);
        return result;
    }

    private int calcMatchScore(Car car, String req) {
        int score = 50; // 基础分
        String usageType = car.getUsageType() != null ? car.getUsageType() : "";

        // ====== 商务接待场景 ======
        if (req.contains("商务") || req.contains("接待") || req.contains("客户") || req.contains("企业")) {
            if (usageType.contains("商务")) {
                score += 40; // 用途匹配，大幅加分
                // 黑色豪华品牌额外加分
                if ("黑色".equals(car.getColor()) && (car.getBrand().contains("奔驰") || car.getBrand().contains("宝马") || car.getBrand().contains("奥迪"))) {
                    score += 15;
                }
                if (car.getBrand().contains("别克") && car.getModel().contains("GL8")) {
                    score += 10; // GL8 商务标杆
                }
            } else {
                score -= 20; // 非商务车辆降分
            }
        }

        // ====== 婚庆用车场景 ======
        if (req.contains("婚") || req.contains("婚礼") || req.contains("结婚") || req.contains("新娘") || req.contains("喜")) {
            if (usageType.contains("婚庆")) {
                score += 45; // 婚庆用途匹配，最高加分
                // 红色/白色额外加分
                if ("红色".equals(car.getColor()) || "白色".equals(car.getColor())) {
                    score += 10;
                }
                // 豪华品牌加分
                if (car.getBrand().contains("保时捷") || car.getBrand().contains("红旗") || car.getBrand().contains("奔驰") || car.getBrand().contains("宝马")) {
                    score += 10;
                }
            } else {
                score -= 25; // 非婚庆车辆大幅降分
            }
        }

        // ====== 家庭出游场景 ======
        if (req.contains("家庭") || req.contains("家人") || req.contains("出游") || req.contains("自驾") || req.contains("旅游")) {
            if (usageType.contains("家庭") || usageType.contains("旅游")) {
                score += 30;
                if (car.getSeats() >= 6) score += 10; // 大空间加分
            }
        }

        // ====== 通勤代步场景 ======
        if (req.contains("通勤") || req.contains("代步") || req.contains("上班") || req.contains("城市")) {
            if (usageType.contains("通勤")) {
                score += 30;
                if (car.getPricePerDay().doubleValue() < 200) score += 10; // 经济车型加分
            }
        }

        // ====== 价格匹配 ======
        if (req.contains("便宜") || req.contains("经济") || req.contains("省钱") || req.contains("预算低")) {
            if (car.getPricePerDay().doubleValue() < 200) score += 25;
            else if (car.getPricePerDay().doubleValue() < 300) score += 10;
        }
        if (req.contains("高档") || req.contains("豪华") || req.contains("高端")) {
            if (car.getPricePerDay().doubleValue() >= 400) score += 25;
            else if (car.getPricePerDay().doubleValue() >= 300) score += 10;
        }

        // ====== 人数匹配 ======
        if (req.contains("6人") || req.contains("6个") || req.contains("六人") || req.contains("大家庭")) {
            if (car.getSeats() >= 6) score += 20;
        }
        if (req.contains("7人") || req.contains("7个") || req.contains("七人") || req.contains("多人")) {
            if (car.getSeats() >= 7) score += 25;
        }

        // ====== 车型匹配 ======
        if ((req.contains("suv") || req.contains("越野")) && "SUV".equals(car.getCategory())) score += 15;
        if ((req.contains("mpv") || req.contains("商务车")) && "MPV".equals(car.getCategory())) score += 15;
        if ((req.contains("电车") || req.contains("纯电") || req.contains("新能源")) && "新能源".equals(car.getCategory())) score += 15;

        // ====== 品牌关键词匹配 ======
        String brandLower = car.getBrand().toLowerCase();
        String modelLower = car.getModel().toLowerCase();
        if (req.contains(brandLower) || req.contains(modelLower)) score += 15;

        return Math.min(98, Math.max(10, score));
    }

    private String buildReason(Car car, String req) {
        List<String> reasons = new ArrayList<>();
        String usageType = car.getUsageType() != null ? car.getUsageType() : "";

        // ====== 商务接待理由 ======
        if (req.contains("商务") || req.contains("接待") || req.contains("客户") || req.contains("企业")) {
            if (usageType.contains("商务")) {
                reasons.add("专业商务接待车型");
                if (car.getBrand().contains("奔驰") || car.getBrand().contains("宝马") || car.getBrand().contains("奥迪")) {
                    reasons.add("豪华品牌，彰显企业实力");
                }
                if ("黑色".equals(car.getColor())) {
                    reasons.add("黑色车身，沉稳大气");
                }
                if (car.getModel().contains("GL8")) {
                    reasons.add("商务MPV标杆，接待客户首选");
                }
                if (car.getSeats() >= 7) {
                    reasons.add(car.getSeats() + "座大空间，多人接待无压力");
                }
            }
        }
        // ====== 婚庆用车理由 ======
        else if (req.contains("婚") || req.contains("婚礼") || req.contains("结婚") || req.contains("新娘")) {
            if (usageType.contains("婚庆")) {
                reasons.add("专业婚庆用车");
                if ("红色".equals(car.getColor())) {
                    reasons.add("红色车身，喜庆吉祥，婚礼头车首选");
                } else if ("白色".equals(car.getColor())) {
                    reasons.add("白色车身，纯洁浪漫，婚礼经典之选");
                }
                if (car.getBrand().contains("保时捷")) {
                    reasons.add("豪华品牌，婚礼尽显尊贵");
                } else if (car.getBrand().contains("红旗")) {
                    reasons.add("国产豪华旗舰，中式婚礼完美搭配");
                }
                reasons.add("为您的大日子增添光彩");
            }
        }
        // ====== 家庭出游理由 ======
        else if (req.contains("家庭") || req.contains("出游") || req.contains("自驾") || req.contains("旅游")) {
            if (usageType.contains("家庭") || usageType.contains("旅游")) {
                reasons.add("家庭出行理想之选");
                if (car.getSeats() >= 7) {
                    reasons.add(car.getSeats() + "座大空间，全家出行舒适");
                } else if (car.getSeats() >= 6) {
                    reasons.add(car.getSeats() + "座宽敞空间，满足家庭需求");
                }
                if ("SUV".equals(car.getCategory())) {
                    reasons.add("SUV车型，通过性强，适合各种路况");
                } else if ("MPV".equals(car.getCategory())) {
                    reasons.add("MPV车型，空间超大，长途舒适");
                }
            }
        }
        // ====== 通勤代步理由 ======
        else if (req.contains("通勤") || req.contains("代步") || req.contains("上班")) {
            if (usageType.contains("通勤")) {
                reasons.add("城市通勤理想之选");
                if (car.getPricePerDay().doubleValue() < 200) {
                    reasons.add("日租仅" + car.getPricePerDay() + "元，经济实惠");
                }
                if ("新能源".equals(car.getCategory())) {
                    reasons.add("新能源车型，用车成本低");
                }
            }
        }
        // ====== 通用理由 ======
        else {
            if (car.getPricePerDay().doubleValue() < 200) reasons.add("经济实惠");
            else if (car.getPricePerDay().doubleValue() < 350) reasons.add("性价比出色");
            else reasons.add("豪华品质之选");
        }

        // 车况补充
        if (car.getMileage() > 0 && car.getMileage() < 20000) {
            reasons.add("准新车状态，车况优秀");
        }

        // 组合理由
        String result = String.join("，", reasons);
        if (result.isEmpty()) {
            result = car.getDescription() != null ? car.getDescription() : "品质可靠，值得信赖";
        }
        return result + "。";
    }
}
