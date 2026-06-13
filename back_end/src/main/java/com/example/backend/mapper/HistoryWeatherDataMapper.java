package com.example.backend.mapper;

import com.example.backend.entity.HistoryWeatherData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface HistoryWeatherDataMapper {

    List<HistoryWeatherData> findAll();

    List<HistoryWeatherData> findByObservationTime(@Param("observationTime") LocalDateTime observationTime);

    List<HistoryWeatherData> findByObservationTimeBetween(@Param("startTime") LocalDateTime startTime,
                                                          @Param("endTime") LocalDateTime endTime);

    List<HistoryWeatherData> findByStationIdAndObservationTimeBetween(@Param("stationId") String stationId,
                                                                       @Param("startTime") LocalDateTime startTime,
                                                                       @Param("endTime") LocalDateTime endTime);

    List<LocalDateTime> findDistinctObservationTimes();

    LocalDateTime findLatestObservationTime();
}
