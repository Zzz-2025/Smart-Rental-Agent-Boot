package dev.langchain4j.example.exception;

/**
 * =========================== 订单未找到异常 ===========================
 *
 * 当用户提供的订单号在数据库中查不到时抛出。
 * 例如：订单号写错了、订单已被取消、订单号不存在。
 *
 * 这个异常会被上层（BookingService）抛出，最终由 Spring 的默认错误处理
 * 转换为 HTTP 500 响应返回给前端。
 */
public class BookingNotFoundException extends RuntimeException {

    public BookingNotFoundException(String bookingNumber) {
        super("Booking " + bookingNumber + " not found");
    }
}
