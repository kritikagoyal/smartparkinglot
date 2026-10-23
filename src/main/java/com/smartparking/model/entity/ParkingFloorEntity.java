package com.smartparking.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "parking_floors")
public class ParkingFloorEntity {

    @Id
    private String id;
    private String lotId;
    private int floorNumber;
    private int totalSpots;

    public ParkingFloorEntity() {}

    public ParkingFloorEntity(String id, String lotId, int floorNumber, int totalSpots) {
        this.id = id;
        this.lotId = lotId;
        this.floorNumber = floorNumber;
        this.totalSpots = totalSpots;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLotId() {
        return lotId;
    }

    public void setLotId(String lotId) {
        this.lotId = lotId;
    }

    public int getFloorNumber() {
        return floorNumber;
    }

    public void setFloorNumber(int floorNumber) {
        this.floorNumber = floorNumber;
    }

    public int getTotalSpots() {
        return totalSpots;
    }

    public void setTotalSpots(int totalSpots) {
        this.totalSpots = totalSpots;
    }
}
