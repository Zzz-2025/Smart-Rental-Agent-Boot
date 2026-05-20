package dev.langchain4j.example.service;

import dev.langchain4j.example.mapper.VehicleMapper;
import dev.langchain4j.example.model.Vehicle;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

/**
 * 车辆业务服务。封装车辆的按牌查询、空闲列表、条件筛选（分类/座位/预算）等操作。
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
