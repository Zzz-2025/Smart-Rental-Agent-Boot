package dev.langchain4j.example.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 续租操作返回结果，包含更新后的还车时间、延长天数、额外费用和总金额。
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
