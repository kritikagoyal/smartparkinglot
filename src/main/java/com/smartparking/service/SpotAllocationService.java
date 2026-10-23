package com.smartparking.service;

import com.smartparking.model.domain.EntryGate;
import com.smartparking.model.domain.Vehicle;
import com.smartparking.model.entity.ParkingSpotEntity;
import com.smartparking.model.enums.ParkingSpotType;
import com.smartparking.model.enums.VehicleType;
import com.smartparking.repository.ParkingSpotRepository;
import com.smartparking.strategy.allocation.SpotAllocationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class SpotAllocationService {

    private static final Logger log = LoggerFactory.getLogger(SpotAllocationService.class);

    private final ParkingSpotRepository spotRepository;
    private volatile SpotAllocationStrategy allocationStrategy;

    // Granular in-memory locks per spot ID to serialize concurrent assignments to the same spot
    private final ConcurrentHashMap<String, ReentrantLock> spotLocks = new ConcurrentHashMap<>();

    public SpotAllocationService(ParkingSpotRepository spotRepository,
                                 SpotAllocationStrategy allocationStrategy) {
        this.spotRepository = Objects.requireNonNull(spotRepository, "ParkingSpotRepository must not be null");
        this.allocationStrategy = Objects.requireNonNull(allocationStrategy, "SpotAllocationStrategy must not be null");
    }

    public SpotAllocationStrategy getAllocationStrategy() {
        return this.allocationStrategy;
    }

    public void setAllocationStrategy(SpotAllocationStrategy strategy) {
        this.allocationStrategy = Objects.requireNonNull(strategy, "Strategy must not be null");
        log.info("Switched active spot allocation strategy to: {}", strategy.getClass().getSimpleName());
    }

    /**
     * Determines compatible spot types for a given vehicle.
     */
    public List<ParkingSpotType> getCompatibleSpotTypes(VehicleType vehicleType) {
        List<ParkingSpotType> compatible = new ArrayList<>();
        for (ParkingSpotType spotType : ParkingSpotType.values()) {
            if (spotType.canFit(vehicleType)) {
                compatible.add(spotType);
            }
        }
        return compatible;
    }

    /**
     * Finds and reserves the best available spot for an incoming vehicle.
     */
    public Optional<ParkingSpotEntity> findBestSpot(Vehicle vehicle, EntryGate gate) {
        List<ParkingSpotType> compatibleTypes = getCompatibleSpotTypes(vehicle.getVehicleType());
        List<ParkingSpotEntity> candidates = spotRepository.findAvailableBySpotTypesOrderedByProximity(compatibleTypes);

        return allocationStrategy.allocateSpot(candidates, vehicle, gate);
    }

    /**
     * Acquires lock on a specific spot for concurrent-safe mutation.
     */
    public ReentrantLock getLockForSpot(String spotId) {
        return spotLocks.computeIfAbsent(spotId, k -> new ReentrantLock());
    }
}
