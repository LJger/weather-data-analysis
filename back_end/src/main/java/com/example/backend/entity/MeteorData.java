package com.example.backend.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 气象要素数据实体 对应表 gra_project.meteor_data
 */
public class MeteorData {

    private Long id;

    private Integer userId;

    private Long taskId;

    private Integer stationId;

    private String elementCode;

    private OffsetDateTime datetime;

    private BigDecimal value;

    public Long getId() {
        return id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Integer getStationId() {
        return stationId;
    }

    public void setStationId(Integer stationId) {
        this.stationId = stationId;
    }

    public String getElementCode() {
        return elementCode;
    }

    public void setElementCode(String elementCode) {
        this.elementCode = elementCode;
    }

    public OffsetDateTime getDatetime() {
        return datetime;
    }

    public void setDatetime(OffsetDateTime datetime) {
        this.datetime = datetime;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }
}


