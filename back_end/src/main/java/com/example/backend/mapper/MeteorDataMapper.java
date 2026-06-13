package com.example.backend.mapper;

import com.example.backend.entity.MeteorData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface MeteorDataMapper {

    void insertBatch(@Param("list") List<MeteorData> list);

    void deleteByTaskId(@Param("taskId") Long taskId);

    long countByUserId(@Param("userId") Integer userId);

    long countDistinctTaskIdByUserId(@Param("userId") Integer userId);

    long countDistinctStationIdByUserId(@Param("userId") Integer userId);

    long countDistinctElementCodeByUserId(@Param("userId") Integer userId);

    List<MeteorData> findTop1000ByUserIdOrderByDatetimeDesc(@Param("userId") Integer userId);

    MeteorData findTop1ByUserIdOrderByDatetimeDesc(@Param("userId") Integer userId);

    List<MeteorData> findTopNByUserIdOrderByDatetimeDesc(@Param("userId") Integer userId,
                                                          @Param("limit") int limit);

    List<MeteorData> findTopNByUserIdAndElementCodeOrderByDatetimeDesc(@Param("userId") Integer userId,
                                                                        @Param("elementCode") String elementCode,
                                                                        @Param("limit") int limit);

    List<Map<String, Object>> aggregateTimeSeries(@Param("userId") Integer userId,
                                                   @Param("elementCode") String elementCode,
                                                   @Param("startTime") OffsetDateTime startTime,
                                                   @Param("endTime") OffsetDateTime endTime,
                                                   @Param("granularity") String granularity,
                                                   @Param("stationIds") Integer[] stationIds,
                                                   @Param("taskIds") Long[] taskIds);

    List<String> findDistinctElementCodesByUserId(@Param("userId") Integer userId);

    List<Integer> findDistinctStationIdsByUserId(@Param("userId") Integer userId);

    List<Map<String, Object>> aggregateSpatialByStation(@Param("userId") Integer userId,
                                                         @Param("elementCode") String elementCode,
                                                         @Param("startTime") OffsetDateTime startTime,
                                                         @Param("endTime") OffsetDateTime endTime);

    List<Map<String, Object>> aggregateProvinceSpatialStats(@Param("userId") Integer userId,
                                                             @Param("elementCode") String elementCode,
                                                             @Param("startTime") OffsetDateTime startTime,
                                                             @Param("endTime") OffsetDateTime endTime);

    List<Map<String, Object>> aggregateStationSpatialStats(@Param("userId") Integer userId,
                                                            @Param("elementCode") String elementCode,
                                                            @Param("startTime") OffsetDateTime startTime,
                                                            @Param("endTime") OffsetDateTime endTime,
                                                            @Param("province") String province);

    List<Map<String, Object>> aggregateCorrelationSeries(@Param("userId") Integer userId,
                                                          @Param("elementCodes") String[] elementCodes,
                                                          @Param("startTime") OffsetDateTime startTime,
                                                          @Param("endTime") OffsetDateTime endTime,
                                                          @Param("granularity") String granularity,
                                                          @Param("stationIds") Integer[] stationIds,
                                                          @Param("taskIds") Long[] taskIds);

    List<MeteorData> findByDatetimeBetween(@Param("startTime") OffsetDateTime startTime,
                                            @Param("endTime") OffsetDateTime endTime);

    List<MeteorData> findByUserIdAndElementCodeAndDatetimeBetweenOrderByDatetimeAsc(
            @Param("userId") Integer userId,
            @Param("elementCode") String elementCode,
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime);

    List<MeteorData> findForPrediction(@Param("userId") Integer userId,
                                        @Param("elementCode") String elementCode,
                                        @Param("startTime") OffsetDateTime startTime,
                                        @Param("endTime") OffsetDateTime endTime,
                                        @Param("stationId") Integer stationId);

    List<Map<String, Object>> findDataTimeRange(@Param("userId") Integer userId,
                                                 @Param("elementCode") String elementCode);

    List<Map<String, Object>> countGroupByElementCode(@Param("userId") Integer userId);

    /** 动态条件分页查询 */
    List<MeteorData> searchRecords(@Param("userId") Integer userId,
                                    @Param("taskIds") List<Long> taskIds,
                                    @Param("stationIds") List<Integer> stationIds,
                                    @Param("elementCodes") List<String> elementCodes,
                                    @Param("startTime") OffsetDateTime startTime,
                                    @Param("endTime") OffsetDateTime endTime,
                                    @Param("offset") int offset,
                                    @Param("limit") int limit);

    long countByFilters(@Param("userId") Integer userId,
                        @Param("taskIds") List<Long> taskIds,
                        @Param("stationIds") List<Integer> stationIds,
                        @Param("elementCodes") List<String> elementCodes,
                        @Param("startTime") OffsetDateTime startTime,
                        @Param("endTime") OffsetDateTime endTime);
}
