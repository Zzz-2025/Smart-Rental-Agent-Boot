package dev.langchain4j.example.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * =========================== 延期结果 ===========================
 *
 * 当用户请求"延期还车"时，系统计算完后返回这个对象。
 *
 * 字段说明：
 *   bookingNumber — 订单号
 *   newEndDate    — 更新后的还车日期
 *   extraDays     — 延长的天数
 *   extraAmount   — 延期产生的额外费用（天数 × 300 元/天）
 *   totalAmount   — 更新后的订单总金额（原金额 + 额外费用）
 */
public class ExtensionResult {

    private String bookingNumber;
    private LocalDate newEndDate;
    private int extraDays;
    private BigDecimal extraAmount;
    private BigDecimal totalAmount;

    public ExtensionResult() {}

    public ExtensionResult(String bookingNumber, LocalDate newEndDate, int extraDays,
                           BigDecimal extraAmount, BigDecimal totalAmount) {
        this.bookingNumber = bookingNumber;
        this.newEndDate = newEndDate;
        this.extraDays = extraDays;
        this.extraAmount = extraAmount;
        this.totalAmount = totalAmount;
    }

    public String bookingNumber() { return bookingNumber; }
    public void setBookingNumber(String bookingNumber) { this.bookingNumber = bookingNumber; }
    public LocalDate newEndDate() { return newEndDate; }
    public void setNewEndDate(LocalDate newEndDate) { this.newEndDate = newEndDate; }
    public int extraDays() { return extraDays; }
    public void setExtraDays(int extraDays) { this.extraDays = extraDays; }
    public BigDecimal extraAmount() { return extraAmount; }
    public void setExtraAmount(BigDecimal extraAmount) { this.extraAmount = extraAmount; }
    public BigDecimal totalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
}
