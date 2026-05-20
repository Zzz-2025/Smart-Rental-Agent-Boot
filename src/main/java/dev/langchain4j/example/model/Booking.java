package dev.langchain4j.example.model;

import java.time.LocalDate;

/**
 * 租车订单实体，对应 bookings 表。包含订单编号、租车时间、客户信息、车辆信息及总金额。
 */
public class Booking {

    private Long id;
    private String bookingNumber;
    private LocalDate bookingBeginDate;
    private LocalDate bookingEndDate;
    private Customer customer;
    private String licensePlate;
    private String vehicleType;
    private String rentalLocation;
    private java.math.BigDecimal totalAmount;

    public Booking() {}

    public Booking(String bookingNumber, LocalDate bookingBeginDate, LocalDate bookingEndDate, Customer customer,
                   String licensePlate, String vehicleType, String rentalLocation) {
        this.bookingNumber = bookingNumber;
        this.bookingBeginDate = bookingBeginDate;
        this.bookingEndDate = bookingEndDate;
        this.customer = customer;
        this.licensePlate = licensePlate;
        this.vehicleType = vehicleType;
        this.rentalLocation = rentalLocation;
    }

    public Long id() { return id; }
    public void setId(Long id) { this.id = id; }
    public String bookingNumber() { return bookingNumber; }
    public void setBookingNumber(String bookingNumber) { this.bookingNumber = bookingNumber; }
    public LocalDate bookingBeginDate() { return bookingBeginDate; }
    public void setBookingBeginDate(LocalDate bookingBeginDate) { this.bookingBeginDate = bookingBeginDate; }
    public LocalDate bookingEndDate() { return bookingEndDate; }
    public void setBookingEndDate(LocalDate bookingEndDate) { this.bookingEndDate = bookingEndDate; }
    public Customer customer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public String licensePlate() { return licensePlate; }
    public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }
    public String vehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }
    public String rentalLocation() { return rentalLocation; }
    public void setRentalLocation(String rentalLocation) { this.rentalLocation = rentalLocation; }
    public java.math.BigDecimal totalAmount() { return totalAmount; }
    public void setTotalAmount(java.math.BigDecimal totalAmount) { this.totalAmount = totalAmount; }
}
