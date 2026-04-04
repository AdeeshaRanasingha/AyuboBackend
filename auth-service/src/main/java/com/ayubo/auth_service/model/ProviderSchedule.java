package com.ayubo.auth_service.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "provider_schedules")
public class ProviderSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "provider_id", nullable = false)
    @JsonIgnore // Prevents infinite loops when sending JSON back to React
    private MedicalProvider provider;

    private String date; // Format: YYYY-MM-DD
    private String startTime; // Format: HH:MM
    private String endTime; // Format: HH:MM
    private Integer maxPatients;
    private String hospitalName;

    public String getHospitalName() { return hospitalName; }
    public void setHospitalName(String hospitalName) { this.hospitalName = hospitalName; }

    // ==========================================
    // GETTERS AND SETTERS
    // ==========================================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public MedicalProvider getProvider() { return provider; }
    public void setProvider(MedicalProvider provider) { this.provider = provider; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public Integer getMaxPatients() { return maxPatients; }
    public void setMaxPatients(Integer maxPatients) { this.maxPatients = maxPatients; }
}