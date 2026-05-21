package dev.langchain4j.example.service;

import dev.langchain4j.example.exception.BookingNotFoundException;
import dev.langchain4j.example.mapper.BookingMapper;
import dev.langchain4j.example.mapper.VehicleMapper;
import dev.langchain4j.example.model.Booking;
import dev.langchain4j.example.model.Customer;
import dev.langchain4j.example.model.ExtensionResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * =========================== 订单业务服务 ===========================
 *
 * 这是订单相关的"业务逻辑层"，每个方法封装了一个完整的业务流程。
 * 它位于"工具层（BookingTools）"和"数据库层（BookingMapper）"之间。
 *
 * 调用链路：
 *   用户提问 → AI Agent → BookingTools(@Tool) → BookingService(业务逻辑) → BookingMapper(SQL) → MySQL
 *
 * 核心方法：
 *   1. 查询：getBookingDetailsByPhone / ByIdNumber
 *   2. 同步创建：createBooking（直接写库，旧版用）
 *   3. 异步处理：processAsyncOrder（RabbitMQ 消费者调用，库存已在锁内扣减）
 *   4. 延期续租：extendBookingByPhone / ByIdNumber
 *   5. 取消：cancelBookingByPhone / ByIdNumber
 *   6. 冲突检查：checkVehicleAvailability
 *
 * @Transactional 注解保证数据库操作的原子性——要么全成功，要么全回滚。
 */
@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);
    private static final BigDecimal DAILY_RATE = new BigDecimal("300");  // 日租金 300 元

    private final BookingMapper bookingMapper;
    private final VehicleMapper vehicleMapper;

    public BookingService(BookingMapper bookingMapper, VehicleMapper vehicleMapper) {
        this.bookingMapper = bookingMapper;
        this.vehicleMapper = vehicleMapper;
    }

    // ==================== 查询订单 ====================

    public Booking getBookingDetailsByPhone(String bookingNumber, String employerPhone) {
        return bookingMapper
                .findByBookingNumberAndEmployerPhone(bookingNumber, employerPhone)
                .orElseThrow(() -> new BookingNotFoundException(bookingNumber));
    }

    public Booking getBookingDetailsByIdNumber(String bookingNumber, String employerIdNumber) {
        return bookingMapper
                .findByBookingNumberAndEmployerIdNumber(bookingNumber, employerIdNumber)
                .orElseThrow(() -> new BookingNotFoundException(bookingNumber));
    }

    // ==================== 同步创建订单（旧版，直接写库） ====================

    @Transactional
    public Booking createBooking(String bookingNumber, String bookingBeginDate, String bookingEndDate,
                                  String customerName, String customerSurname,
                                  String employerName, String employerPhone, String employerIdNumber,
                                  String licensePlate, String vehicleType, String rentalLocation) {
        LocalDate begin = LocalDate.parse(bookingBeginDate);
        LocalDate end = LocalDate.parse(bookingEndDate);
        long days = ChronoUnit.DAYS.between(begin, end);
        BigDecimal totalAmount = DAILY_RATE.multiply(BigDecimal.valueOf(days));

        Booking booking = new Booking(
                bookingNumber, begin, end,
                new Customer(customerName, customerSurname, employerName, employerPhone, employerIdNumber),
                licensePlate, vehicleType, rentalLocation
        );
        booking.setTotalAmount(totalAmount);
        bookingMapper.save(booking);
        return booking;
    }

    // ==================== 车辆冲突检查（延期时用） ====================

    /**
     * 检查指定时间段内是否有其他订单占用了这辆车。
     * 排除当前订单自身（excludeBookingNumber），只查其他订单。
     *
     * @return true=空闲可用, false=已被占用
     */
    public boolean checkVehicleAvailability(String licensePlate, String excludeBookingNumber,
                                             String beginDate, String endDate) {
        LocalDate begin = LocalDate.parse(beginDate);
        LocalDate end = LocalDate.parse(endDate);
        int count = bookingMapper.countOverlappingBookings(licensePlate, excludeBookingNumber, begin, end);
        return count == 0;
    }

    // ==================== 延期还车 ====================

    @Transactional
    public ExtensionResult extendBookingByPhone(String bookingNumber, String employerPhone, String newEndDate) {
        Booking booking = bookingMapper
                .findByBookingNumberAndEmployerPhone(bookingNumber, employerPhone)
                .orElseThrow(() -> new BookingNotFoundException(bookingNumber));
        return doExtend(booking, newEndDate);
    }

    @Transactional
    public ExtensionResult extendBookingByIdNumber(String bookingNumber, String employerIdNumber, String newEndDate) {
        Booking booking = bookingMapper
                .findByBookingNumberAndEmployerIdNumber(bookingNumber, employerIdNumber)
                .orElseThrow(() -> new BookingNotFoundException(bookingNumber));
        return doExtend(booking, newEndDate);
    }

    /**
     * 延期核心逻辑：计算额外天数 → 计算额外费用 → 更新数据库。
     */
    private ExtensionResult doExtend(Booking booking, String newEndDate) {
        LocalDate newEnd = LocalDate.parse(newEndDate);
        LocalDate oldEnd = booking.bookingEndDate();
        int extraDays = (int) ChronoUnit.DAYS.between(oldEnd, newEnd);

        if (extraDays <= 0) {
            throw new IllegalArgumentException("新还车日期必须晚于当前还车日期 " + oldEnd);
        }

        BigDecimal extraAmount = DAILY_RATE.multiply(BigDecimal.valueOf(extraDays));
        BigDecimal currentTotal = booking.totalAmount() != null ? booking.totalAmount() : BigDecimal.ZERO;
        BigDecimal newTotal = currentTotal.add(extraAmount);

        bookingMapper.updateBookingEndDateAndAmount(booking.bookingNumber(), newEnd, newTotal);

        return new ExtensionResult(booking.bookingNumber(), newEnd, extraDays, extraAmount, newTotal);
    }

    // ==================== 取消订单 ====================

    @Transactional
    public void cancelBookingByPhone(String bookingNumber, String employerPhone) {
        bookingMapper
                .findByBookingNumberAndEmployerPhone(bookingNumber, employerPhone)
                .orElseThrow(() -> new BookingNotFoundException(bookingNumber));
        bookingMapper.deleteByBookingNumberAndEmployerPhone(bookingNumber, employerPhone);
    }

    @Transactional
    public void cancelBookingByIdNumber(String bookingNumber, String employerIdNumber) {
        bookingMapper
                .findByBookingNumberAndEmployerIdNumber(bookingNumber, employerIdNumber)
                .orElseThrow(() -> new BookingNotFoundException(bookingNumber));
        bookingMapper.deleteByBookingNumberAndEmployerIdNumber(bookingNumber, employerIdNumber);
    }
}
