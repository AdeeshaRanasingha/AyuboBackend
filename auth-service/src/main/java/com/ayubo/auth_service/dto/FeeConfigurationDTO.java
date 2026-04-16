package com.ayubo.auth_service.dto;

import java.util.List;

public class FeeConfigurationDTO {
    private double taxPercentage;
    private double onlineConsultationFee;
    private List<HospitalDTO> hospitalFees;

    // Getters and Setters
    public double getTaxPercentage() { return taxPercentage; }
    public void setTaxPercentage(double taxPercentage) { this.taxPercentage = taxPercentage; }
    public double getOnlineConsultationFee() { return onlineConsultationFee; }
    public void setOnlineConsultationFee(double onlineConsultationFee) { this.onlineConsultationFee = onlineConsultationFee; }
    public List<HospitalDTO> getHospitalFees() { return hospitalFees; }
    public void setHospitalFees(List<HospitalDTO> hospitalFees) { this.hospitalFees = hospitalFees; }

    public static class HospitalDTO {
        private Long id;
        private String name;
        private double fee;

        public HospitalDTO(Long id, String name, double fee) {
            this.id = id;
            this.name = name;
            this.fee = fee;
        }

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public double getFee() { return fee; }
        public void setFee(double fee) { this.fee = fee; }
    }
}