package com.healthcare.appointmentservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "appoinment")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "appointment_number", nullable = false, unique = true, length = 50)
    private String appointmentNumber;

    /**
     * Optional legacy numeric id; new bookings use patientEmail as the source of truth.
     */
    @Column(name = "patient_id")
    private Long patientId;

    @Column(name = "patient_email", nullable = false, length = 255)
    private String patientEmail;

    @Column(name = "appointment_for", length = 50)
    private String appointmentFor;

    @Column(name = "appointment_type", length = 50)
    private String appointmentType;

    @Column(name = "patient_title", length = 20)
    private String patientTitle;

    @Column(name = "patient_name", length = 150)
    private String patientName;

    @Column(name = "contact_number", length = 30)
    private String contactNumber;

    @Column(name = "identification_type", length = 30)
    private String identificationType;

    @Column(name = "identification_value", length = 100)
    private String identificationValue;

    @Column(name = "contact_email", length = 255)
    private String contactEmail;

    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;

    @Column(name = "slot_id")
    private Long slotId;

    @Column(name = "specialty", length = 100)
    private String specialty;

    @Column(name = "appointment_date", nullable = false)
    private LocalDate appointmentDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "note_or_address", length = 255)
    private String noteOrAddress;

    @Column(name = "on_going_number")
    private Boolean onGoingNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private AppointmentStatus status;

    @Column(name = "payment_status", length = 50)
    private String paymentStatus;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "total_price", precision = 10, scale = 2)
    private java.math.BigDecimal totalPrice;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // Add this near your other fields (like noteOrAddress)
    @Column(name = "prescription_url")
    private String prescriptionUrl;

    // Add the Getter and Setter at the bottom
    public String getPrescriptionUrl() {
        return prescriptionUrl;
    }

    public void setPrescriptionUrl(String prescriptionUrl) {
        this.prescriptionUrl = prescriptionUrl;
    }

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.onGoingNumber == null) {
            this.onGoingNumber = false;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
