package com.smartparking.model.domain;

import com.smartparking.model.enums.VehicleType;

public final class VehicleFactory {
    private VehicleFactory() {}

    public static Vehicle createVehicle(String licensePlate, VehicleType type) {
        if (type == null) {
            throw new IllegalArgumentException("Vehicle type cannot be null");
        }
        return switch (type) {
            case MOTORCYCLE -> new Motorcycle(licensePlate);
            case CAR -> new Car(licensePlate);
            case BUS -> new Bus(licensePlate);
        };
    }
}
