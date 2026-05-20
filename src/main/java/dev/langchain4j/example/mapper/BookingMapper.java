package dev.langchain4j.example.mapper;

import dev.langchain4j.example.model.Booking;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

/**
 * 订单 MyBatis Mapper。定义订单的查询、创建、延期、删除等数据库操作接口，
 * SQL 实现见 resources/mapper/BookingMapper.xml。
 */
@Mapper
public interface BookingMapper {

    Optional<Booking> findByBookingNumberAndEmployerPhone(
            @Param("bookingNumber") String bookingNumber,
            @Param("employerPhone") String employerPhone);

    Optional<Booking> findByBookingNumberAndEmployerIdNumber(
            @Param("bookingNumber") String bookingNumber,
            @Param("employerIdNumber") String employerIdNumber);

    void save(Booking booking);

    int countOverlappingBookings(@Param("licensePlate") String licensePlate,
                                  @Param("excludeBookingNumber") String excludeBookingNumber,
                                  @Param("beginDate") LocalDate beginDate,
                                  @Param("endDate") LocalDate endDate);

    void updateBookingEndDateAndAmount(@Param("bookingNumber") String bookingNumber,
                                        @Param("newEndDate") LocalDate newEndDate,
                                        @Param("totalAmount") BigDecimal totalAmount);

    void deleteByBookingNumberAndEmployerPhone(
            @Param("bookingNumber") String bookingNumber,
            @Param("employerPhone") String employerPhone);

    void deleteByBookingNumberAndEmployerIdNumber(
            @Param("bookingNumber") String bookingNumber,
            @Param("employerIdNumber") String employerIdNumber);
}
