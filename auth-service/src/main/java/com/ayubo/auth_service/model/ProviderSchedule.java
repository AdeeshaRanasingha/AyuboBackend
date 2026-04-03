package com.ayubo.auth_service.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "doctor_schedule_slots")
public class ProviderSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "provider_id", nullable = false)
    @JsonIgnore // Prevents infinite loops when sending JSON back to React
    private MedicalProvider provider;

    @Column(name = "slot_date", nullable = false)
    @JsonIgnore
    private LocalDate slotDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    @JsonIgnore
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @JsonIgnore
    private LocalDateTime updatedAt;

    private String date; // Format: YYYY-MM-DD
    private String startTime; // Format: HH:MM
    private String endTime; // Format: HH:MM
    private Integer maxPatients;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        syncSlotDate();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
        syncSlotDate();
    }

    private void syncSlotDate() {
        if (date != null && !date.isBlank()) {
            slotDate = LocalDate.parse(date);
        } else if (slotDate != null) {
            date = slotDate.toString();
        }
    }

    // ==========================================
    // GETTERS AND SETTERS
    // ==========================================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public MedicalProvider getProvider() { return provider; }
    public void setProvider(MedicalProvider provider) { this.provider = provider; }

    public String getDate() {
        return date != null ? date : (slotDate != null ? slotDate.toString() : null);
    }
    public void setDate(String date) {
        this.date = date;
        syncSlotDate();
    }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public Integer getMaxPatients() { return maxPatients; }
    public void setMaxPatients(Integer maxPatients) { this.maxPatients = maxPatients; }
}
