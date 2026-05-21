package dev.langchain4j.example.model;

import java.math.BigDecimal;

/**
 * =========================== 车辆实体 ===========================
 *
 * 对应数据库的 vehicles 表，主键为车牌号（license_plate）。
 *
 * 字段说明：
 *   licensePlate      — 车牌号（主键，唯一标识一辆车）
 *   vehicleType       — 车辆型号名称，如"丰田汉兰达"
 *   category          — 车型分类：轿车、SUV、MPV、豪华、越野
 *   seats             — 座位数
 *   availableQuantity — 当前空闲数量（可租的辆数）
 *   totalQuantity     — 总数量（空闲 + 已租出）
 *   dailyRate         — 日租金（元/天）
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
