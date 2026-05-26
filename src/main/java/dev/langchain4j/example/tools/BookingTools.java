package dev.langchain4j.example.tools;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.example.mapper.VehicleMapper;
import dev.langchain4j.example.model.Booking;
import dev.langchain4j.example.model.ExtensionResult;
import dev.langchain4j.example.model.Vehicle;
import dev.langchain4j.example.service.BookingService;
import dev.langchain4j.example.service.VehicleService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * =========================== 订单工具组件 ===========================
 *
 * 这个类定义了 AI 客服能够执行的"订单相关操作"，每个 @Tool 方法都会被
 * LangChain4j 框架暴露给大模型调用。当用户说"帮我查订单 MS-123"时，
 * AI 不会自己编造答案，而是自动调用对应的 @Tool 方法去数据库查询。
 *
 * 支持的操作：
 *   1. 查询订单（按电话 或 身份证号）
 *   2. 创建预订（含 Redis 分布式锁防超卖 + 同步写入）
 *   3. 检查车辆可用性
 *   4. 延期还车（按电话 或 身份证号）
 *   5. 取消订单（按电话 或 身份证号）
 *
 * 并发安全保障（createBooking）：
 *   锁内（Redis 分布式锁保护）：
 *     a) 查询车辆库存 → b) 校验库存 > 0 → c) 立即扣减库存
 *   锁外：
 *     d) 同步写入订单记录
 *
 *   库存扣减在锁内完成，彻底杜绝"超卖"问题。
 */
@Component
public class BookingTools {

    private static final Logger log = LoggerFactory.getLogger(BookingTools.class);
    private static final BigDecimal DAILY_RATE = new BigDecimal("300");     // 日租金 300 元
    private static final String LOCK_PREFIX = "car:lock:";                  // 分布式锁 Key 前缀

    private final BookingService bookingService;
    private final VehicleService vehicleService;
    private final VehicleMapper vehicleMapper;
    private final RedissonClient redissonClient;

    public BookingTools(BookingService bookingService,
                        VehicleService vehicleService,
                        VehicleMapper vehicleMapper,
                        RedissonClient redissonClient) {
        this.bookingService = bookingService;
        this.vehicleService = vehicleService;
        this.vehicleMapper = vehicleMapper;
        this.redissonClient = redissonClient;
    }

    // ==================== 查询订单 ====================

    @Tool("通过订单号和雇主联系电话查询订单")
    public Booking getBookingDetailsByPhone(String bookingNumber, String employerPhone) {
        return bookingService.getBookingDetailsByPhone(bookingNumber, employerPhone);
    }

    @Tool("通过订单号和雇主身份证号查询订单")
    public Booking getBookingDetailsByIdNumber(String bookingNumber, String employerIdNumber) {
        return bookingService.getBookingDetailsByIdNumber(bookingNumber, employerIdNumber);
    }

    // ==================== 创建订单（核心流程 + 并发安全） ====================

    @Tool("创建租车订单（含分布式锁防超卖，同步写入数据库）")
    public String createBooking(String bookingNumber, String bookingBeginDate, String bookingEndDate,
                                String customerName, String customerSurname,
                                String employerName, String employerPhone, String employerIdNumber,
                                String licensePlate, String vehicleType, String rentalLocation) {
        // ---- 加锁：针对车牌号加 Redis 分布式锁，防止同一辆车被多人同时下单 ----
        String lockKey = LOCK_PREFIX + licensePlate;
        RLock lock = redissonClient.getLock(lockKey);
        log.info("尝试获取分布式锁: key={}, bookingNumber={}", lockKey, bookingNumber);

        try {
            lock.lock();
            log.info("分布式锁获取成功: key={}", lockKey);

            // ---- 锁内步骤1：查询车辆信息 ----
            Vehicle vehicle = vehicleService.getVehicleByLicensePlate(licensePlate);
            if (vehicle == null || vehicle.availableQuantity() == null || vehicle.availableQuantity() <= 0) {
                throw new RuntimeException("抱歉，车辆 " + licensePlate + " 当前已无空闲库存，无法下单。请选择其他车型或稍后再试。");
            }

            // ---- 锁内步骤2：解析日期并计算金额 ----
            LocalDate begin = LocalDate.parse(bookingBeginDate);
            LocalDate end = LocalDate.parse(bookingEndDate);
            long days = ChronoUnit.DAYS.between(begin, end);
            if (days <= 0) {
                throw new RuntimeException("租车结束日期必须晚于开始日期。");
            }
            BigDecimal totalAmount = DAILY_RATE.multiply(BigDecimal.valueOf(days));

            // ---- 锁内步骤3：立即扣减库存（核心：检查和扣减在同一个锁内完成） ----
            int decremented = vehicleMapper.decrementAvailableQuantity(licensePlate);
            if (decremented == 0) {
                throw new RuntimeException("抱歉，车辆 " + licensePlate + " 库存已空，下单失败。请稍后再试。");
            }
            log.info("库存扣减成功: licensePlate={}, bookingNumber={}", licensePlate, bookingNumber);

            // ---- 锁内步骤4：同步写入订单到数据库 ----
            bookingService.createBooking(bookingNumber, bookingBeginDate, bookingEndDate,
                    customerName, customerSurname,
                    employerName, employerPhone, employerIdNumber,
                    licensePlate, vehicleType, rentalLocation);
            log.info("订单同步写入完成: bookingNumber={}", bookingNumber);

            return "订单 " + bookingNumber + " 创建成功！"
                    + "预计租车" + days + "天，预估金额" + totalAmount + "元。"
                    + "您可以通过订单号随时查询订单详情。";

        } finally {
            // ---- 释放锁（finally 保证即使抛异常也会释放） ----
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("分布式锁已释放: key={}", lockKey);
            }
        }
    }

    // ==================== 车辆可用性检查（用于延期时判断） ====================

    @Tool("检查指定车辆在延期时间段内是否有其他订单冲突，返回 true 表示空闲可续租")
    public boolean checkVehicleAvailability(String licensePlate, String excludeBookingNumber,
                                             String beginDate, String endDate) {
        return bookingService.checkVehicleAvailability(licensePlate, excludeBookingNumber, beginDate, endDate);
    }

    // ==================== 延期还车 ====================

    @Tool("通过订单号和雇主联系电话延长租车时间")
    public ExtensionResult extendBookingByPhone(String bookingNumber, String employerPhone, String newEndDate) {
        return bookingService.extendBookingByPhone(bookingNumber, employerPhone, newEndDate);
    }

    @Tool("通过订单号和雇主身份证号延长租车时间")
    public ExtensionResult extendBookingByIdNumber(String bookingNumber, String employerIdNumber, String newEndDate) {
        return bookingService.extendBookingByIdNumber(bookingNumber, employerIdNumber, newEndDate);
    }

    // ==================== 取消订单 ====================

    @Tool("通过订单号和雇主联系电话取消订单")
    public void cancelBookingByPhone(String bookingNumber, String employerPhone) {
        bookingService.cancelBookingByPhone(bookingNumber, employerPhone);
    }

    @Tool("通过订单号和雇主身份证号取消订单")
    public void cancelBookingByIdNumber(String bookingNumber, String employerIdNumber) {
        bookingService.cancelBookingByIdNumber(bookingNumber, employerIdNumber);
    }
}
