package dev.langchain4j.example.service;

import dev.langchain4j.example.exception.BookingNotFoundException;
import dev.langchain4j.example.mapper.BookingMapper;
import dev.langchain4j.example.model.Booking;
import dev.langchain4j.example.model.Customer;
import dev.langchain4j.example.model.ExtensionResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 订单业务服务。封装订单的查询、创建、延期续租、取消等核心业务流程。
 * 延期时自动校验车辆空闲并计算额外费用，日租金按 300 元/天计。
 */
@Service
public class BookingService {

    private static final BigDecimal DAILY_RATE = new BigDecimal("300");

    private final BookingMapper bookingMapper;

    public BookingService(BookingMapper bookingMapper) {
        this.bookingMapper = bookingMapper;
    }

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

    public boolean checkVehicleAvailability(String licensePlate, String excludeBookingNumber,
                                             String beginDate, String endDate) {
        LocalDate begin = LocalDate.parse(beginDate);
        LocalDate end = LocalDate.parse(endDate);
        int count = bookingMapper.countOverlappingBookings(licensePlate, excludeBookingNumber, begin, end);
        return count == 0;
    }

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
