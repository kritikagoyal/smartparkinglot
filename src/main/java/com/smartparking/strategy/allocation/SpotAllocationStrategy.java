package com.smartparking.strategy.allocation;

import com.smartparking.model.domain.EntryGate;
import com.smartparking.model.domain.Vehicle;
import com.smartparking.model.entity.ParkingSpotEntity;

import java.util.List;
import java.util.Optional;

public interface SpotAllocationStrategy {
    /**
     * Determines and allocates an optimal parking spot for the incoming vehicle.
     *
     * @param availableSpots list of currently available spots
     * @param vehicle incoming vehicle
     * @param entryGate gate from which vehicle entered
     * @return Optional containing the chosen parking spot, or empty if none available
     */
    Optional<ParkingSpotEntity> allocateSpot(List<ParkingSpotEntity> availableSpots, Vehicle vehicle, EntryGate entryGate);
}
