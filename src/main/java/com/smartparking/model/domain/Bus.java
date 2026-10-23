package com.smartparking.model.domain;

import com.smartparking.model.enums.VehicleType;

public class Bus extends Vehicle {
    public Bus(String licensePlate) {
        super(licensePlate, VehicleType.BUS);
    }
}
