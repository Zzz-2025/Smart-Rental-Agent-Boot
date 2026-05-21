package dev.langchain4j.example.mapper;

import dev.langchain4j.example.model.Booking;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

/**
 * =========================== 订单数据库映射接口 ===========================
 *
 * MyBatis 的 Mapper 接口——定义了操作 bookings 表的所有方法。
 * 不需要写实现类！MyBatis 会在运行时根据同名的 XML 文件
 * （resources/mapper/BookingMapper.xml）自动生成 SQL 实现。
 *
 * 方法一览：
 *   查询：   findByBookingNumberAndEmployerPhone / ByIdNumber
 *   创建：   save
 *   冲突检查：countOverlappingBookings（延期时判断车辆是否空闲）
 *   延期：   updateBookingEndDateAndAmount
 *   取消：   deleteByBookingNumberAndEmployerPhone / ByIdNumber
 */
@Mapper
public interface BookingMapper {

    // 按订单号 + 雇主电话查询（用于身份验证）
    Optional<Booking> findByBookingNumberAndEmployerPhone(
            @Param("bookingNumber") String bookingNumber,
            @Param("employerPhone") String employerPhone);

    // 按订单号 + 雇主身份证号查询（用于身份验证）
    Optional<Booking> findByBookingNumberAndEmployerIdNumber(
            @Param("bookingNumber") String bookingNumber,
            @Param("employerIdNumber") String employerIdNumber);

    // 插入新订单
    void save(Booking booking);

    // 延期时检查车辆时间冲突：统计同一车牌在目标时间段内有多少其他订单
    int countOverlappingBookings(@Param("licensePlate") String licensePlate,
                                  @Param("excludeBookingNumber") String excludeBookingNumber,
                                  @Param("beginDate") LocalDate beginDate,
                                  @Param("endDate") LocalDate endDate);

    // 更新订单的还车日期和总金额
    void updateBookingEndDateAndAmount(@Param("bookingNumber") String bookingNumber,
                                        @Param("newEndDate") LocalDate newEndDate,
                                        @Param("totalAmount") BigDecimal totalAmount);

    // 按订单号 + 雇主电话删除订单
    void deleteByBookingNumberAndEmployerPhone(
            @Param("bookingNumber") String bookingNumber,
            @Param("employerPhone") String employerPhone);

    // 按订单号 + 雇主身份证号删除订单
    void deleteByBookingNumberAndEmployerIdNumber(
            @Param("bookingNumber") String bookingNumber,
            @Param("employerIdNumber") String employerIdNumber);
}
