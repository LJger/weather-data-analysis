package com.example.backend.mapper;

import com.example.backend.entity.WeatherStation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WeatherStationMapper {

    WeatherStation findById(@Param("stationId") String stationId);

    List<WeatherStation> findAll();

    List<WeatherStation> findAllByIds(@Param("stationIds") List<String> stationIds);

    List<String> findDistinctProvinces();

    List<WeatherStation> findByProvinceOrderByStationNameAsc(@Param("province") String province);
}
