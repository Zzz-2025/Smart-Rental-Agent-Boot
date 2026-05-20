package dev.langchain4j.example.tools;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.example.model.Vehicle;
import dev.langchain4j.example.service.VehicleService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * 车辆工具组件。将 VehicleService 封装为 AI Agent 可调用的 @Tool 方法，
 * 包括按牌查车、空闲列表、条件筛选（分类/座位/预算）。
 */
@Component
public class VehicleTools {

    private final VehicleService vehicleService;

    public VehicleTools(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @Tool("根据车牌号码查询车辆详细信息")
    public Vehicle getVehicleByLicensePlate(String licensePlate) {
        return vehicleService.getVehicleByLicensePlate(licensePlate);
    }

    @Tool("查询所有当前有空闲数量的车辆列表")
    public List<Vehicle> getAvailableVehicles() {
        return vehicleService.getAvailableVehicles();
    }

    @Tool("根据车型分类、最少座位数、最高日租金筛选空闲车辆。所有参数均可为空，按日租金升序排列")
    public List<Vehicle> queryAvailableVehicles(String category, Integer minSeats, BigDecimal maxDailyRate) {
        return vehicleService.queryAvailableVehicles(category, minSeats, maxDailyRate);
    }
}
