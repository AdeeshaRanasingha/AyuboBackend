package com.ayubo.auth_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "medical_providers")

public class MedicalProvider extends User {

    private String specialty;
    private String medicalLicenseNumber;
    @Column(length = 1000)// Gives doctors enough space for a good bio
    private String bio;
    // Add these right under your existing variables (specialty, bio, etc.)
    @Column(columnDefinition = "boolean default false")
    private Boolean isApproved = false;

    private Double consultationFee = 0.0;

    private String hospitalName;
    private Integer yearsOfExperience;
    private String qualifications;

    // --- GETTERS & SETTERS ---
    public String getHospitalName() { return hospitalName; }
    public void setHospitalName(String hospitalName) { this.hospitalName = hospitalName; }

    public Integer getYearsOfExperience() { return yearsOfExperience; }
    public void setYearsOfExperience(Integer yearsOfExperience) { this.yearsOfExperience = yearsOfExperience; }

    public String getQualifications() { return qualifications; }
    public void setQualifications(String qualifications) { this.qualifications = qualifications; }

    // --- Add the Getters and Setters ---
    public Boolean getIsApproved() { return isApproved; }
    public void setIsApproved(Boolean isApproved) { this.isApproved = isApproved; }

    public Double getConsultationFee() { return consultationFee; }
    public void setConsultationFee(Double consultationFee) { this.consultationFee = consultationFee; }

    public String getSpecialty() {
        return specialty;
    }

    public void setSpecialty(String specialty) {
        this.specialty = specialty;
    }

    public String getMedicalLicenseNumber() {
        return medicalLicenseNumber;
    }

    public void setMedicalLicenseNumber(String medicalLicenseNumber) {
        this.medicalLicenseNumber = medicalLicenseNumber;
    }
    // Add Getters and Setters
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

}