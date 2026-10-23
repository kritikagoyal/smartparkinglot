package com.smartparking.model.enums;

public enum ParkingSpotType {
    MOTORCYCLE,
    COMPACT,
    LARGE;

    public boolean canFit(VehicleType vehicleType) {
        return switch (this) {
            case MOTORCYCLE -> vehicleType == VehicleType.MOTORCYCLE;
            case COMPACT -> vehicleType == VehicleType.CAR || vehicleType == VehicleType.MOTORCYCLE;
            case LARGE -> true; // Large spot can accommodate any vehicle
        };
    }
}
