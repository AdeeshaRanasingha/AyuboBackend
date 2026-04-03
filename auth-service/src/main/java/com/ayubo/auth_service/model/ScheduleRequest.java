package com.ayubo.auth_service.model;

public class ScheduleRequest {
    private String date;
    private String startTime;
    private String endTime;
    private Integer maxPatients;

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public Integer getMaxPatients() { return maxPatients; }
    public void setMaxPatients(Integer maxPatients) { this.maxPatients = maxPatients; }
}