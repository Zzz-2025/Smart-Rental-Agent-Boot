package dev.langchain4j.example.service;

import dev.langchain4j.example.mapper.VehicleMapper;
import dev.langchain4j.example.model.Vehicle;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

/**
 * =========================== 车辆业务服务 ===========================
 *
 * 封装车辆查询相关的业务逻辑，位于工具层和数据库层之间。
 *
 * 三个查询能力：
 *   1. 按车牌精确查车（下单前验证车辆存在）
 *   2. 查所有有库存的车
 *   3. 按条件筛选（AI 推荐车型时的核心查询）
 */
@Service
public class VehicleService {

    private final VehicleMapper vehicleMapper;

    public VehicleService(VehicleMapper vehicleMapper) {
        this.vehicleMapper = vehicleMapper;
    }

    public Vehicle getVehicleByLicensePlate(String licensePlate) {
        return vehicleMapper.findByLicensePlate(licensePlate)
                .orElseThrow(() -> new RuntimeException("车辆不存在: " + licensePlate));
    }

    public List<Vehicle> getAvailableVehicles() {
        return vehicleMapper.findAllAvailable();
    }

    public List<Vehicle> queryAvailableVehicles(String category, Integer minSeats, BigDecimal maxDailyRate) {
        return vehicleMapper.queryAvailableVehicles(category, minSeats, maxDailyRate);
    }
}
