package dev.langchain4j.example.tools;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.example.model.Booking;
import dev.langchain4j.example.model.ExtensionResult;
import dev.langchain4j.example.service.BookingService;
import org.springframework.stereotype.Component;

/**
 * 订单工具组件。将 BookingService 封装为 AI Agent 可调用的 @Tool 方法，
 * 包括查询、创建、延期续租（含空闲校验）、取消订单。
 */
@Component
public class BookingTools {

    private final BookingService bookingService;

    public BookingTools(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Tool("通过订单号和雇主联系电话查询订单")
    public Booking getBookingDetailsByPhone(String bookingNumber, String employerPhone) {
        return bookingService.getBookingDetailsByPhone(bookingNumber, employerPhone);
    }

    @Tool("通过订单号和雇主身份证号查询订单")
    public Booking getBookingDetailsByIdNumber(String bookingNumber, String employerIdNumber) {
        return bookingService.getBookingDetailsByIdNumber(bookingNumber, employerIdNumber);
    }

    @Tool
    public Booking createBooking(String bookingNumber, String bookingBeginDate, String bookingEndDate,
                                  String customerName, String customerSurname,
                                  String employerName, String employerPhone, String employerIdNumber,
                                  String licensePlate, String vehicleType, String rentalLocation) {
        return bookingService.createBooking(bookingNumber, bookingBeginDate, bookingEndDate,
                customerName, customerSurname,
                employerName, employerPhone, employerIdNumber,
                licensePlate, vehicleType, rentalLocation);
    }

    @Tool("检查指定车辆在延期时间段内是否有其他订单冲突，返回 true 表示空闲可续租")
    public boolean checkVehicleAvailability(String licensePlate, String excludeBookingNumber,
                                             String beginDate, String endDate) {
        return bookingService.checkVehicleAvailability(licensePlate, excludeBookingNumber, beginDate, endDate);
    }

    @Tool("通过订单号和雇主联系电话延长租车时间，自动计算额外费用并更新订单结束日期和总金额")
    public ExtensionResult extendBookingByPhone(String bookingNumber, String employerPhone, String newEndDate) {
        return bookingService.extendBookingByPhone(bookingNumber, employerPhone, newEndDate);
    }

    @Tool("通过订单号和雇主身份证号延长租车时间，自动计算额外费用并更新订单结束日期和总金额")
    public ExtensionResult extendBookingByIdNumber(String bookingNumber, String employerIdNumber, String newEndDate) {
        return bookingService.extendBookingByIdNumber(bookingNumber, employerIdNumber, newEndDate);
    }

    @Tool("通过订单号和雇主联系电话取消订单")
    public void cancelBookingByPhone(String bookingNumber, String employerPhone) {
        bookingService.cancelBookingByPhone(bookingNumber, employerPhone);
    }

    @Tool("通过订单号和雇主身份证号取消订单")
    public void cancelBookingByIdNumber(String bookingNumber, String employerIdNumber) {
        bookingService.cancelBookingByIdNumber(bookingNumber, employerIdNumber);
    }
}
