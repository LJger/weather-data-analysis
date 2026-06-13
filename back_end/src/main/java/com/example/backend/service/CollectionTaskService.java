package com.example.backend.service;

import com.example.backend.dto.CollectionTaskResponse;
import com.example.backend.dto.CreateCollectionTaskRequest;
import com.example.backend.dto.UpdateTaskStatusRequest;
import com.example.backend.dto.WeatherApiRequest;
import com.example.backend.entity.CollectionTask;
import com.example.backend.mapper.CollectionTaskMapper;
import com.example.backend.mapper.MeteorDataMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Locale;

@Service
public class CollectionTaskService {

    private static final Logger log = LoggerFactory.getLogger(CollectionTaskService.class);
    private static final DateTimeFormatter CMA_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss", Locale.CHINA);

    private final CollectionTaskMapper collectionTaskMapper;
    private final MeteorDataMapper meteorDataMapper;
    private final WeatherApiService weatherApiService;

    public CollectionTaskService(CollectionTaskMapper collectionTaskMapper,
                                 MeteorDataMapper meteorDataMapper,
                                 WeatherApiService weatherApiService) {
        this.collectionTaskMapper = collectionTaskMapper;
        this.meteorDataMapper = meteorDataMapper;
        this.weatherApiService = weatherApiService;
    }

    public CollectionTaskResponse createTask(CreateCollectionTaskRequest request) {
        validateTimeRange(request.startTime(), request.endTime());

        CollectionTask task = new CollectionTask();
        task.setTaskName(request.taskName());
        task.setUserId(request.userId());
        task.setStartTime(request.startTime());
        task.setEndTime(request.endTime());
        task.setStationList(request.stationList().stream()
                .filter(Objects::nonNull)
                .toArray(Integer[]::new));
        task.setElementList(request.elementList().stream()
                .filter(Objects::nonNull)
                .toArray(String[]::new));
        task.setTaskDescription(request.taskDescription());
        task.setStatus(2); // 运行中

        collectionTaskMapper.insert(task);

        try {
            weatherApiService.callWeatherApi(buildWeatherApiRequest(request, task.getTaskId()));
            task.setStatus(1); // 成功
            task.setUpdatedAt(OffsetDateTime.now());
            collectionTaskMapper.update(task);
        } catch (Exception e) {
            log.error("采集任务调用气象 API 失败，taskId={}", task.getTaskId(), e);
            task.setStatus(0); // 失败
            task.setUpdatedAt(OffsetDateTime.now());
            collectionTaskMapper.update(task);
            throw e;
        }

        return mapToResponse(task);
    }

    public List<CollectionTaskResponse> listTasks(Integer userId) {
        return collectionTaskMapper.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public CollectionTaskResponse getTask(Long taskId) {
        CollectionTask task = collectionTaskMapper.findById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在");
        }
        return mapToResponse(task);
    }

    public CollectionTaskResponse updateStatus(Long taskId, UpdateTaskStatusRequest request) {
        CollectionTask task = collectionTaskMapper.findById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在");
        }
        task.setStatus(request.status());
        task.setUpdatedAt(OffsetDateTime.now());
        collectionTaskMapper.update(task);
        return mapToResponse(task);
    }

    @Transactional
    public void deleteTask(Long taskId) {
        if (!collectionTaskMapper.existsById(taskId)) {
            throw new IllegalArgumentException("任务不存在");
        }
        meteorDataMapper.deleteByTaskId(taskId);
        collectionTaskMapper.deleteById(taskId);
    }

    private void validateTimeRange(OffsetDateTime start, OffsetDateTime end) {
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("开始时间不能晚于结束时间");
        }
    }

    private WeatherApiRequest buildWeatherApiRequest(CreateCollectionTaskRequest request, Long taskId) {
        List<String> staIDs = request.stationList().stream()
                .map(String::valueOf)
                .toList();
        String timeRange = "[" +
                request.startTime().withOffsetSameInstant(ZoneOffset.UTC).format(CMA_DATE_TIME_FORMATTER) +
                "," +
                request.endTime().withOffsetSameInstant(ZoneOffset.UTC).format(CMA_DATE_TIME_FORMATTER) +
                "]";

        return new WeatherApiRequest(
                staIDs,
                timeRange,
                request.elementList(),
                "weather_station",
                "hourly",
                request.taskDescription(),
                taskId,
                request.userId()
        );
    }

    private CollectionTaskResponse mapToResponse(CollectionTask task) {
        return new CollectionTaskResponse(
                task.getTaskId(),
                task.getTaskName(),
                task.getUserId(),
                task.getStartTime(),
                task.getEndTime(),
                task.stationList(),
                task.elementList(),
                task.getTaskDescription(),
                task.getStatus(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}


