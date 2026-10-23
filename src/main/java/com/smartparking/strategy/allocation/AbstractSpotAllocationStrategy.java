package com.smartparking.strategy.allocation;

import com.smartparking.model.domain.Vehicle;
import com.smartparking.model.entity.ParkingSpotEntity;
import com.smartparking.model.enums.ParkingSpotType;
import com.smartparking.model.enums.VehicleType;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Common base strategy consolidating validation, filtering, and fit-tier calculations
 * to avoid duplication across allocation strategies (DRY).
 */
public abstract class AbstractSpotAllocationStrategy implements SpotAllocationStrategy {

    /**
     * Shared filter ensuring spot is available and compatible with the vehicle.
     */
    protected Stream<ParkingSpotEntity> filterCompatibleAvailableSpots(List<ParkingSpotEntity> spots, Vehicle vehicle) {
        if (spots == null || spots.isEmpty() || vehicle == null) {
            return Stream.empty();
        }
        VehicleType type = Objects.requireNonNull(vehicle.getVehicleType(), "Vehicle type must not be null");
        return spots.stream()
                .filter(spot -> spot != null && spot.isAvailable() && spot.getSpotType().canFit(type));
    }

    /**
     * Fit Tier:
     * Tier 0: Perfect match (e.g., Car -> Compact, Bus -> Large, Moto -> Moto)
     * Tier 1: 1-level fallback (e.g., Moto -> Compact, Car -> Large)
     * Tier 2: 2-level fallback (e.g., Moto -> Large)
     */
    protected int getFitTier(VehicleType vehicleType, ParkingSpotType spotType) {
        if (vehicleType == null || spotType == null) {
            return 99;
        }
        return switch (vehicleType) {
            case MOTORCYCLE -> switch (spotType) {
                case MOTORCYCLE -> 0;
                case COMPACT -> 1;
                case LARGE -> 2;
            };
            case CAR -> switch (spotType) {
                case COMPACT -> 0;
                case LARGE -> 1;
                default -> 99;
            };
            case BUS -> switch (spotType) {
                case LARGE -> 0;
                default -> 99;
            };
        };
    }
}
