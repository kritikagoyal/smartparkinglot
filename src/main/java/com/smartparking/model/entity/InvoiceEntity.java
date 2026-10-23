package com.smartparking.model.entity;

import com.smartparking.model.enums.PaymentStatus;
import com.smartparking.model.enums.VehicleType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoices", indexes = {
    @Index(name = "idx_invoice_ticket", columnList = "ticketId"),
    @Index(name = "idx_invoice_plate", columnList = "licensePlate")
})
public class InvoiceEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String ticketId;

    @Column(nullable = false)
    private String licensePlate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleType vehicleType;

    @Column(nullable = false)
    private String spotId;

    @Column(nullable = false)
    private String exitGateId;

    @Column(nullable = false)
    private LocalDateTime entryTime;

    @Column(nullable = false)
    private LocalDateTime exitTime;

    @Column(nullable = false)
    private long durationMinutes;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal baseFee;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal hourlyFee;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal vehicleMultiplier;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public InvoiceEntity() {}

    public InvoiceEntity(String id, String ticketId, String licensePlate, VehicleType vehicleType,
                         String spotId, String exitGateId, LocalDateTime entryTime, LocalDateTime exitTime,
                         long durationMinutes, BigDecimal baseFee, BigDecimal hourlyFee,
                         BigDecimal vehicleMultiplier, BigDecimal totalAmount,
                         PaymentStatus paymentStatus) {
        this.id = id;
        this.ticketId = ticketId;
        this.licensePlate = licensePlate;
        this.vehicleType = vehicleType;
        this.spotId = spotId;
        this.exitGateId = exitGateId;
        this.entryTime = entryTime;
        this.exitTime = exitTime;
        this.durationMinutes = durationMinutes;
        this.baseFee = baseFee;
        this.hourlyFee = hourlyFee;
        this.vehicleMultiplier = vehicleMultiplier;
        this.totalAmount = totalAmount;
        this.paymentStatus = paymentStatus;
        this.createdAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTicketId() {
        return ticketId;
    }

    public void setTicketId(String ticketId) {
        this.ticketId = ticketId;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(VehicleType vehicleType) {
        this.vehicleType = vehicleType;
    }

    public String getSpotId() {
        return spotId;
    }

    public void setSpotId(String spotId) {
        this.spotId = spotId;
    }

    public String getExitGateId() {
        return exitGateId;
    }

    public void setExitGateId(String exitGateId) {
        this.exitGateId = exitGateId;
    }

    public LocalDateTime getEntryTime() {
        return entryTime;
    }

    public void setEntryTime(LocalDateTime entryTime) {
        this.entryTime = entryTime;
    }

    public LocalDateTime getExitTime() {
        return exitTime;
    }

    public void setExitTime(LocalDateTime exitTime) {
        this.exitTime = exitTime;
    }

    public long getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(long durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public BigDecimal getBaseFee() {
        return baseFee;
    }

    public void setBaseFee(BigDecimal baseFee) {
        this.baseFee = baseFee;
    }

    public BigDecimal getHourlyFee() {
        return hourlyFee;
    }

    public void setHourlyFee(BigDecimal hourlyFee) {
        this.hourlyFee = hourlyFee;
    }

    public BigDecimal getVehicleMultiplier() {
        return vehicleMultiplier;
    }

    public void setVehicleMultiplier(BigDecimal vehicleMultiplier) {
        this.vehicleMultiplier = vehicleMultiplier;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
