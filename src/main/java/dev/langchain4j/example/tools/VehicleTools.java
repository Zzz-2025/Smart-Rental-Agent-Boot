package dev.langchain4j.example.tools;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.example.model.Vehicle;
import dev.langchain4j.example.service.VehicleService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * =========================== 车辆工具组件 ===========================
 *
 * 为 AI Agent 提供车辆查询能力。每个 @Tool 方法都可以被大模型自主调用。
 *
 * 三个工具方法：
 *   1. 按车牌查车   — 精确查询某一辆车的详情
 *   2. 查空闲车辆   — 列出所有有库存的车
 *   3. 条件筛选     — 按分类/座位数/预算过滤，用于智能推荐
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
