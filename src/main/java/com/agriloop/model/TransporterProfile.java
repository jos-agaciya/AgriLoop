package com.agriloop.model;

import java.math.BigDecimal;

/**
 * Extended profile details for Transporter stakeholders.
 */
public class TransporterProfile extends BaseEntity {
    private Long userId;
    private String vehicleType;
    private String vehicleNumber;
    private BigDecimal maxPayloadTons;
    private BigDecimal operatingRadiusKm;
    private String licenseNumber;
    private boolean available;

    public TransporterProfile() {
        this.available = true;
        this.operatingRadiusKm = new BigDecimal("50.00");
    }

    public TransporterProfile(Long userId, String vehicleType, String vehicleNumber, BigDecimal maxPayloadTons, String licenseNumber) {
        this();
        this.userId = userId;
        this.vehicleType = vehicleType;
        this.vehicleNumber = vehicleNumber;
        this.maxPayloadTons = maxPayloadTons;
        this.licenseNumber = licenseNumber;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public String getVehicleNumber() { return vehicleNumber; }
    public void setVehicleNumber(String vehicleNumber) { this.vehicleNumber = vehicleNumber; }

    public BigDecimal getMaxPayloadTons() { return maxPayloadTons; }
    public void setMaxPayloadTons(BigDecimal maxPayloadTons) { this.maxPayloadTons = maxPayloadTons; }

    public BigDecimal getOperatingRadiusKm() { return operatingRadiusKm; }
    public void setOperatingRadiusKm(BigDecimal operatingRadiusKm) { this.operatingRadiusKm = operatingRadiusKm; }

    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
}
