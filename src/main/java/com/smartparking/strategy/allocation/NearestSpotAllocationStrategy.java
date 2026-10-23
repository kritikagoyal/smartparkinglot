package com.smartparking.strategy.allocation;

import com.smartparking.model.domain.EntryGate;
import com.smartparking.model.domain.Vehicle;
import com.smartparking.model.entity.ParkingSpotEntity;
import com.smartparking.model.enums.VehicleType;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Assigns the nearest available spot prioritizing lowest floor, shortest gate distance,
 * and spot fit tier.
 */
@Component("nearestSpotAllocationStrategy")
@Primary
public class NearestSpotAllocationStrategy extends AbstractSpotAllocationStrategy {

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
                        .thenComparingInt(ParkingSpotEntity::getDistanceMetric)
                        .thenComparingInt(ParkingSpotEntity::getSpotNumber));
    }
}
