package com.smartparking.model.entity;

import com.smartparking.model.enums.TicketStatus;
import com.smartparking.model.enums.VehicleType;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "parking_tickets", indexes = {
    @Index(name = "idx_ticket_plate", columnList = "licensePlate"),
    @Index(name = "idx_ticket_status", columnList = "status")
})
public class ParkingTicketEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String licensePlate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleType vehicleType;

    @Column(nullable = false)
    private String spotId;

    @Column(nullable = false)
    private int floorNumber;

    @Column(nullable = false)
    private String entryGateId;

    @Column(nullable = false)
    private LocalDateTime entryTime;

    @Column(nullable = true)
    private LocalDateTime exitTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus status;

    public ParkingTicketEntity() {}

    public ParkingTicketEntity(String id, String licensePlate, VehicleType vehicleType,
                               String spotId, int floorNumber, String entryGateId,
                               LocalDateTime entryTime) {
        this.id = id;
        this.licensePlate = licensePlate;
        this.vehicleType = vehicleType;
        this.spotId = spotId;
        this.floorNumber = floorNumber;
        this.entryGateId = entryGateId;
        this.entryTime = entryTime;
        this.status = TicketStatus.ACTIVE;
    }

    public void complete(LocalDateTime exitTime) {
        this.exitTime = exitTime;
        this.status = TicketStatus.COMPLETED;
    }

    public void cancel() {
        this.status = TicketStatus.CANCELLED;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public int getFloorNumber() {
        return floorNumber;
    }

    public void setFloorNumber(int floorNumber) {
        this.floorNumber = floorNumber;
    }

    public String getEntryGateId() {
        return entryGateId;
    }

    public void setEntryGateId(String entryGateId) {
        this.entryGateId = entryGateId;
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

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }
}
