package dev.langchain4j.example.model;

import java.math.BigDecimal;

/**
 * 车辆实体，对应 vehicles 表。主键为车牌号，包含车型分类、座位数、空闲/总数量和日租金。
 */
public class Vehicle {

    private String licensePlate;
    private String vehicleType;
    private String category;
    private Integer seats;
    private Integer availableQuantity;
    private Integer totalQuantity;
    private BigDecimal dailyRate;

    public Vehicle() {}

    public String licensePlate() { return licensePlate; }
    public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }
    public String vehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }
    public String category() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Integer seats() { return seats; }
    public void setSeats(Integer seats) { this.seats = seats; }
    public Integer availableQuantity() { return availableQuantity; }
    public void setAvailableQuantity(Integer availableQuantity) { this.availableQuantity = availableQuantity; }
    public Integer totalQuantity() { return totalQuantity; }
    public void setTotalQuantity(Integer totalQuantity) { this.totalQuantity = totalQuantity; }
    public BigDecimal dailyRate() { return dailyRate; }
    public void setDailyRate(BigDecimal dailyRate) { this.dailyRate = dailyRate; }
}
