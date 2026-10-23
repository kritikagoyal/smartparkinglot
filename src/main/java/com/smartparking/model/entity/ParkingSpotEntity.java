package com.smartparking.model.entity;

import com.smartparking.model.enums.ParkingSpotStatus;
import com.smartparking.model.enums.ParkingSpotType;
import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "parking_spots", indexes = {
    @Index(name = "idx_spot_status_type", columnList = "status, spotType"),
    @Index(name = "idx_spot_floor", columnList = "floorNumber")
})
public class ParkingSpotEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private int floorNumber;

    @Column(nullable = false)
    private int spotNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParkingSpotType spotType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParkingSpotStatus status;

    /**
     * Distance metric (e.g. distance units from entrance gate) used by allocation algorithms.
     */
    @Column(nullable = false)
    private int distanceMetric;

    @Column(nullable = true)
    private String currentVehiclePlate;

    @Version
    private Long version;

    public ParkingSpotEntity() {}

    public ParkingSpotEntity(String id, int floorNumber, int spotNumber, ParkingSpotType spotType, int distanceMetric) {
        this.id = id;
        this.floorNumber = floorNumber;
        this.spotNumber = spotNumber;
        this.spotType = spotType;
        this.distanceMetric = distanceMetric;
        this.status = ParkingSpotStatus.AVAILABLE;
        this.currentVehiclePlate = null;
    }

    public boolean isAvailable() {
        return this.status == ParkingSpotStatus.AVAILABLE;
    }

    public void occupy(String licensePlate) {
        if (this.status != ParkingSpotStatus.AVAILABLE) {
            throw new IllegalStateException("Cannot occupy spot " + id + " because it is currently " + status);
        }
        this.status = ParkingSpotStatus.OCCUPIED;
        this.currentVehiclePlate = licensePlate;
    }

    public void vacate() {
        this.status = ParkingSpotStatus.AVAILABLE;
        this.currentVehiclePlate = null;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getFloorNumber() {
        return floorNumber;
    }

    public void setFloorNumber(int floorNumber) {
        this.floorNumber = floorNumber;
    }

    public int getSpotNumber() {
        return spotNumber;
    }

    public void setSpotNumber(int spotNumber) {
        this.spotNumber = spotNumber;
    }

    public ParkingSpotType getSpotType() {
        return spotType;
    }

    public void setSpotType(ParkingSpotType spotType) {
        this.spotType = spotType;
    }

    public ParkingSpotStatus getStatus() {
        return status;
    }

    public void setStatus(ParkingSpotStatus status) {
        this.status = status;
    }

    public int getDistanceMetric() {
        return distanceMetric;
    }

    public void setDistanceMetric(int distanceMetric) {
        this.distanceMetric = distanceMetric;
    }

    public String getCurrentVehiclePlate() {
        return currentVehiclePlate;
    }

    public void setCurrentVehiclePlate(String currentVehiclePlate) {
        this.currentVehiclePlate = currentVehiclePlate;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ParkingSpotEntity that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ParkingSpotEntity{" +
                "id='" + id + '\'' +
                ", floorNumber=" + floorNumber +
                ", spotNumber=" + spotNumber +
                ", spotType=" + spotType +
                ", status=" + status +
                ", distanceMetric=" + distanceMetric +
                ", currentVehiclePlate='" + currentVehiclePlate + '\'' +
                '}';
    }
}
