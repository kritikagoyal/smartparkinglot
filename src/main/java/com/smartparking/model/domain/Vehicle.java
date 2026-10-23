package com.smartparking.model.domain;

import com.smartparking.model.enums.VehicleType;
import java.util.Objects;

public abstract class Vehicle {
    private final String licensePlate;
    private final VehicleType vehicleType;

    protected Vehicle(String licensePlate, VehicleType vehicleType) {
        if (licensePlate == null || licensePlate.isBlank()) {
            throw new IllegalArgumentException("License plate must not be null or blank");
        }
        this.licensePlate = licensePlate.trim().toUpperCase();
        this.vehicleType = Objects.requireNonNull(vehicleType, "VehicleType must not be null");
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Vehicle vehicle)) return false;
        return Objects.equals(licensePlate, vehicle.licensePlate) && vehicleType == vehicle.vehicleType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(licensePlate, vehicleType);
    }

    @Override
    public String toString() {
        return "Vehicle{" +
                "licensePlate='" + licensePlate + '\'' +
                ", vehicleType=" + vehicleType +
                '}';
    }
}
