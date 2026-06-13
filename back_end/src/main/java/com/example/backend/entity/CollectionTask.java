package com.example.backend.entity;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 数据采集任务实体 对应表 gra_project.collection_tasks
 */
public class CollectionTask {

    private Long taskId;

    private String taskName;

    private Integer userId;

    private OffsetDateTime startTime;

    private OffsetDateTime endTime;

    private Integer[] stationList;

    private String[] elementList;

    private String taskDescription;

    private Integer status;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;

    public Long getTaskId() {
        return taskId;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public OffsetDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(OffsetDateTime startTime) {
        this.startTime = startTime;
    }

    public OffsetDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(OffsetDateTime endTime) {
        this.endTime = endTime;
    }

    public Integer[] getStationList() {
        return stationList;
    }

    public void setStationList(Integer[] stationList) {
        this.stationList = stationList;
    }

    public List<Integer> stationList() {
        return stationList == null ? List.of() : Arrays.asList(stationList);
    }

    public String[] getElementList() {
        return elementList;
    }

    public void setElementList(String[] elementList) {
        this.elementList = elementList;
    }

    public List<String> elementList() {
        return elementList == null ? List.of() : Arrays.asList(elementList);
    }

    public String getTaskDescription() {
        return taskDescription;
    }

    public void setTaskDescription(String taskDescription) {
        this.taskDescription = taskDescription;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}


