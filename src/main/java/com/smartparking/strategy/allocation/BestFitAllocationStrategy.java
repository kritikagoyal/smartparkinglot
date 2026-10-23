package com.smartparking.strategy.allocation;

import com.smartparking.model.domain.EntryGate;
import com.smartparking.model.domain.Vehicle;
import com.smartparking.model.entity.ParkingSpotEntity;
import com.smartparking.model.enums.VehicleType;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Strategy that strictly optimizes spot capacity utilization (Best-Fit).
 * It prioritizes exact matching spots to avoid consuming larger spots for smaller vehicles.
 */
@Component("bestFitAllocationStrategy")
public class BestFitAllocationStrategy extends AbstractSpotAllocationStrategy {

    @Override
    public Optional<ParkingSpotEntity> allocateSpot(List<ParkingSpotEntity> availableSpots,
                                                    Vehicle vehicle,
                                                    EntryGate entryGate) {
        if (availableSpots == null || availableSpots.isEmpty() || vehicle == null) {
            return Optional.empty();
        }

        VehicleType vehicleType = vehicle.getVehicleType();

        return filterCompatibleAvailableSpots(availableSpots, vehicle)
                .min(Comparator
                        .comparingInt((ParkingSpotEntity s) -> getFitTier(vehicleType, s.getSpotType()))
                        .thenComparingInt(ParkingSpotEntity::getFloorNumber)
                        .thenComparingInt(ParkingSpotEntity::getSpotNumber));
    }
}
