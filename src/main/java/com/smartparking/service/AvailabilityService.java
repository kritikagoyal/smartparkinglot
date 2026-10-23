package com.smartparking.service;

import com.smartparking.dto.AvailabilityResponse;
import com.smartparking.dto.FloorAvailabilityDto;
import com.smartparking.model.entity.ParkingFloorEntity;
import com.smartparking.model.entity.ParkingLotEntity;
import com.smartparking.model.entity.ParkingSpotEntity;
import com.smartparking.model.enums.ParkingSpotStatus;
import com.smartparking.model.enums.ParkingSpotType;
import com.smartparking.model.event.SpotStatusChangedEvent;
import com.smartparking.repository.ParkingFloorRepository;
import com.smartparking.repository.ParkingLotRepository;
import com.smartparking.repository.ParkingSpotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Real-time availability service acting as an Observer / Event Listener for spot state transitions.
 * Maintains concurrent in-memory counters for instantaneous O(1) reads across display boards and APIs.
 */
@Service
public class AvailabilityService {

    private static final Logger log = LoggerFactory.getLogger(AvailabilityService.class);

    private final ParkingSpotRepository spotRepository;
    private final ParkingFloorRepository floorRepository;
    private final ParkingLotRepository lotRepository;

    // Floor -> SpotType -> Count of available spots
    private final Map<Integer, Map<ParkingSpotType, AtomicLong>> floorTypeAvailableCounts = new ConcurrentHashMap<>();
    // Floor -> Total capacity
    private final Map<Integer, Long> floorCapacity = new ConcurrentHashMap<>();

    public AvailabilityService(ParkingSpotRepository spotRepository,
                               ParkingFloorRepository floorRepository,
                               ParkingLotRepository lotRepository) {
        this.spotRepository = spotRepository;
        this.floorRepository = floorRepository;
        this.lotRepository = lotRepository;
    }

    public synchronized void initializeAvailability() {
        floorTypeAvailableCounts.clear();
        floorCapacity.clear();

        List<ParkingFloorEntity> floors = floorRepository.findAllByOrderByFloorNumberAsc();
        for (ParkingFloorEntity floor : floors) {
            int floorNum = floor.getFloorNumber();
            floorCapacity.put(floorNum, (long) floor.getTotalSpots());

            Map<ParkingSpotType, AtomicLong> typeMap = new ConcurrentHashMap<>();
            for (ParkingSpotType type : ParkingSpotType.values()) {
                long count = spotRepository.countByFloorNumberAndSpotTypeAndStatus(floorNum, type, ParkingSpotStatus.AVAILABLE);
                typeMap.put(type, new AtomicLong(count));
            }
            floorTypeAvailableCounts.put(floorNum, typeMap);
        }
        log.info("Initialized real-time availability cache for {} floors.", floors.size());
    }

    @EventListener
    public void onSpotStatusChanged(SpotStatusChangedEvent event) {
        int floorNum = event.floorNumber();
        ParkingSpotType type = event.spotType();

        floorTypeAvailableCounts.putIfAbsent(floorNum, new ConcurrentHashMap<>());
        Map<ParkingSpotType, AtomicLong> typeMap = floorTypeAvailableCounts.get(floorNum);
        typeMap.putIfAbsent(type, new AtomicLong(0));

        if (event.oldStatus() == ParkingSpotStatus.AVAILABLE && event.newStatus() != ParkingSpotStatus.AVAILABLE) {
            typeMap.get(type).decrementAndGet();
            log.debug("Real-time Availability Decremented: Spot {} on Floor {} ({}) occupied.",
                    event.spotId(), floorNum, type);
        } else if (event.oldStatus() != ParkingSpotStatus.AVAILABLE && event.newStatus() == ParkingSpotStatus.AVAILABLE) {
            typeMap.get(type).incrementAndGet();
            log.debug("Real-time Availability Incremented: Spot {} on Floor {} ({}) vacated.",
                    event.spotId(), floorNum, type);
        }
    }

    public AvailabilityResponse getAvailability() {
        ParkingLotEntity lot = lotRepository.findAll().stream().findFirst()
                .orElse(new ParkingLotEntity("LOT-1", "Central Smart Parking Lot", "Downtown Metro", 3));

        List<FloorAvailabilityDto> floorDtos = new ArrayList<>();
        long totalAvailable = 0;
        long totalCap = 0;

        List<ParkingFloorEntity> floors = floorRepository.findAllByOrderByFloorNumberAsc();
        for (ParkingFloorEntity floor : floors) {
            int fNum = floor.getFloorNumber();
            Map<ParkingSpotType, AtomicLong> typeMap = floorTypeAvailableCounts.get(fNum);

            long moto = typeMap != null && typeMap.containsKey(ParkingSpotType.MOTORCYCLE)
                    ? Math.max(0, typeMap.get(ParkingSpotType.MOTORCYCLE).get()) : 0;
            long compact = typeMap != null && typeMap.containsKey(ParkingSpotType.COMPACT)
                    ? Math.max(0, typeMap.get(ParkingSpotType.COMPACT).get()) : 0;
            long large = typeMap != null && typeMap.containsKey(ParkingSpotType.LARGE)
                    ? Math.max(0, typeMap.get(ParkingSpotType.LARGE).get()) : 0;

            long floorAvail = moto + compact + large;
            long cap = floorCapacity.getOrDefault(fNum, (long) floor.getTotalSpots());

            totalAvailable += floorAvail;
            totalCap += cap;

            floorDtos.add(new FloorAvailabilityDto(fNum, moto, compact, large, floorAvail, cap));
        }

        return new AvailabilityResponse(lot.getId(), lot.getName(), totalAvailable, totalCap, floorDtos);
    }
}
