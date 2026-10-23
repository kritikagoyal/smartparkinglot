package com.smartparking.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "parking_lots")
public class ParkingLotEntity {

    @Id
    private String id;
    private String name;
    private String address;
    private int totalFloors;

    public ParkingLotEntity() {}

    public ParkingLotEntity(String id, String name, String address, int totalFloors) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.totalFloors = totalFloors;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getTotalFloors() {
        return totalFloors;
    }

    public void setTotalFloors(int totalFloors) {
        this.totalFloors = totalFloors;
    }
}
