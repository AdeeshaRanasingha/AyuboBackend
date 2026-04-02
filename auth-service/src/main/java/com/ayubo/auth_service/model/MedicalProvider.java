package com.ayubo.auth_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "medical_providers")

public class MedicalProvider extends User {

    private String specialty;
    private String medicalLicenseNumber;
    private String hospitalName;
    @Column(length = 1000)// Gives doctors enough space for a good bio
    private String bio;

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
    public String getHospitalName() {
        return hospitalName;
    }

    public void setHospitalName(String hospitalName) {
        this.hospitalName = hospitalName;
    }
    // Add Getters and Setters
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

}
