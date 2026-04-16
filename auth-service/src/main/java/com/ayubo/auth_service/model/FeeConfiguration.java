package com.ayubo.auth_service.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class FeeConfiguration {

    @Id
    private Long id = 1L; // Only one row will ever exist

    // Global Settings
    private double taxPercentage;
    private double onlineConsultationFee;

    // Specific Hospital Fees
    private double asiriHospitalFee;
    private double nawalokaHospitalFee;
    private double lankaHospitalFee;
    private double durdansHospitalFee;
    private double hemasHospitalFee;
    private double medihelpHospitalFee;
    private double ninewellsHospitalFee;
    private double kingsHospitalFee;

    public FeeConfiguration() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public double getTaxPercentage() { return taxPercentage; }
    public void setTaxPercentage(double taxPercentage) { this.taxPercentage = taxPercentage; }

    public double getOnlineConsultationFee() { return onlineConsultationFee; }
    public void setOnlineConsultationFee(double onlineConsultationFee) { this.onlineConsultationFee = onlineConsultationFee; }

    public double getAsiriHospitalFee() { return asiriHospitalFee; }
    public void setAsiriHospitalFee(double asiriHospitalFee) { this.asiriHospitalFee = asiriHospitalFee; }

    public double getNawalokaHospitalFee() { return nawalokaHospitalFee; }
    public void setNawalokaHospitalFee(double nawalokaHospitalFee) { this.nawalokaHospitalFee = nawalokaHospitalFee; }

    public double getLankaHospitalFee() { return lankaHospitalFee; }
    public void setLankaHospitalFee(double lankaHospitalFee) { this.lankaHospitalFee = lankaHospitalFee; }

    public double getDurdansHospitalFee() { return durdansHospitalFee; }
    public void setDurdansHospitalFee(double durdansHospitalFee) { this.durdansHospitalFee = durdansHospitalFee; }

    public double getHemasHospitalFee() { return hemasHospitalFee; }
    public void setHemasHospitalFee(double hemasHospitalFee) { this.hemasHospitalFee = hemasHospitalFee; }

    public double getMedihelpHospitalFee() { return medihelpHospitalFee; }
    public void setMedihelpHospitalFee(double medihelpHospitalFee) { this.medihelpHospitalFee = medihelpHospitalFee; }

    public double getNinewellsHospitalFee() { return ninewellsHospitalFee; }
    public void setNinewellsHospitalFee(double ninewellsHospitalFee) { this.ninewellsHospitalFee = ninewellsHospitalFee; }

    public double getKingsHospitalFee() { return kingsHospitalFee; }
    public void setKingsHospitalFee(double kingsHospitalFee) { this.kingsHospitalFee = kingsHospitalFee; }
}