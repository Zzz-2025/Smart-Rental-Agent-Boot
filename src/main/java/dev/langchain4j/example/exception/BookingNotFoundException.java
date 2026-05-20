package dev.langchain4j.example.exception;

/**
 * 订单未找到异常。当通过订单号查询不到对应订单时抛出。
 */
public class BookingNotFoundException extends RuntimeException {

    public BookingNotFoundException(String bookingNumber) {
        super("Booking " + bookingNumber + " not found");
    }
}
