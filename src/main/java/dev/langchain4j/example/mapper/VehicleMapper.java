package dev.langchain4j.example.mapper;

import dev.langchain4j.example.model.Vehicle;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 车辆 MyBatis Mapper。定义车辆的按车牌查询、空闲查询、条件筛选等操作，
 * SQL 实现见 resources/mapper/VehicleMapper.xml。
 */
@Mapper
public interface VehicleMapper {

    Optional<Vehicle> findByLicensePlate(@Param("licensePlate") String licensePlate);

    List<Vehicle> findAllAvailable();

    List<Vehicle> queryAvailableVehicles(@Param("category") String category,
                                          @Param("minSeats") Integer minSeats,
                                          @Param("maxDailyRate") BigDecimal maxDailyRate);
}
