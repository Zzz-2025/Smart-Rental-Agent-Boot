package dev.langchain4j.example.mapper;

import dev.langchain4j.example.model.Vehicle;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * =========================== 车辆数据库映射接口 ===========================
 *
 * MyBatis Mapper — 操作 vehicles 表。SQL 在 resources/mapper/VehicleMapper.xml。
 *
 * 方法一览：
 *   按车牌查：         findByLicensePlate
 *   查所有空闲：       findAllAvailable
 *   条件筛选+推荐：    queryAvailableVehicles
 *   扣减库存：         decrementAvailableQuantity（CAS 原子更新，防超卖）
 *
 * decrementAvailableQuantity 的 SQL：
 *   UPDATE vehicles SET available_quantity = available_quantity - 1
 *   WHERE license_plate = ? AND available_quantity > 0
 * 这利用了 MySQL InnoDB 的行锁，保证并发安全。
 */
@Mapper
public interface VehicleMapper {

    // 按车牌号精确查询
    Optional<Vehicle> findByLicensePlate(@Param("licensePlate") String licensePlate);

    // 查所有还有空闲库存的车辆
    List<Vehicle> findAllAvailable();

    // 条件筛选：分类、最少座位数、最高日租金（参数均可为空）
    List<Vehicle> queryAvailableVehicles(@Param("category") String category,
                                          @Param("minSeats") Integer minSeats,
                                          @Param("maxDailyRate") BigDecimal maxDailyRate);

    // 原子扣减库存（available_quantity - 1），WHERE 条件保证不会扣成负数
    int decrementAvailableQuantity(@Param("licensePlate") String licensePlate);
}
