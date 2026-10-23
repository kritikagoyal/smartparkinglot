package com.smartparking.service;

import com.smartparking.model.entity.ParkingFloorEntity;
import com.smartparking.model.entity.ParkingLotEntity;
import com.smartparking.model.entity.ParkingSpotEntity;
import com.smartparking.model.enums.ParkingSpotType;
import com.smartparking.repository.ParkingFloorRepository;
import com.smartparking.repository.ParkingLotRepository;
import com.smartparking.repository.ParkingSpotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class InitDataService implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(InitDataService.class);

    private final ParkingLotRepository lotRepository;
    private final ParkingFloorRepository floorRepository;
    private final ParkingSpotRepository spotRepository;
    private final AvailabilityService availabilityService;

    public InitDataService(ParkingLotRepository lotRepository,
                           ParkingFloorRepository floorRepository,
                           ParkingSpotRepository spotRepository,
                           AvailabilityService availabilityService) {
        this.lotRepository = lotRepository;
        this.floorRepository = floorRepository;
        this.spotRepository = spotRepository;
        this.availabilityService = availabilityService;
    }

    @Override
    public void run(String... args) {
        initializeLot();
    }

    public synchronized void initializeLot() {
        if (lotRepository.count() > 0) {
            log.info("Parking lot data already initialized.");
            availabilityService.initializeAvailability();
            return;
        }

        log.info("Initializing multi-floor smart parking lot structure...");

        ParkingLotEntity lot = new ParkingLotEntity(
                "LOT-1",
                "Metro Urban Smart Parking Garage",
                "100 Downtown Center Blvd",
                3
        );
        lotRepository.save(lot);

        List<ParkingFloorEntity> floors = new ArrayList<>();
        List<ParkingSpotEntity> spots = new ArrayList<>();

        // Floor 1: 10 spots (4 Motorcycle, 4 Compact, 2 Large)
        floors.add(new ParkingFloorEntity("FL-1", lot.getId(), 1, 10));
        for (int i = 1; i <= 4; i++) {
            spots.add(new ParkingSpotEntity("F1-M" + i, 1, i, ParkingSpotType.MOTORCYCLE, 10 + i * 2));
        }
        for (int i = 1; i <= 4; i++) {
            spots.add(new ParkingSpotEntity("F1-C" + i, 1, 4 + i, ParkingSpotType.COMPACT, 20 + i * 2));
        }
        for (int i = 1; i <= 2; i++) {
            spots.add(new ParkingSpotEntity("F1-L" + i, 1, 8 + i, ParkingSpotType.LARGE, 30 + i * 2));
        }

        // Floor 2: 10 spots (2 Motorcycle, 6 Compact, 2 Large)
        floors.add(new ParkingFloorEntity("FL-2", lot.getId(), 2, 10));
        for (int i = 1; i <= 2; i++) {
            spots.add(new ParkingSpotEntity("F2-M" + i, 2, i, ParkingSpotType.MOTORCYCLE, 50 + i * 2));
        }
        for (int i = 1; i <= 6; i++) {
            spots.add(new ParkingSpotEntity("F2-C" + i, 2, 2 + i, ParkingSpotType.COMPACT, 60 + i * 2));
        }
        for (int i = 1; i <= 2; i++) {
            spots.add(new ParkingSpotEntity("F2-L" + i, 2, 8 + i, ParkingSpotType.LARGE, 70 + i * 2));
        }

        // Floor 3: 10 spots (2 Motorcycle, 6 Compact, 2 Large)
        floors.add(new ParkingFloorEntity("FL-3", lot.getId(), 3, 10));
        for (int i = 1; i <= 2; i++) {
            spots.add(new ParkingSpotEntity("F3-M" + i, 3, i, ParkingSpotType.MOTORCYCLE, 90 + i * 2));
        }
        for (int i = 1; i <= 6; i++) {
            spots.add(new ParkingSpotEntity("F3-C" + i, 3, 2 + i, ParkingSpotType.COMPACT, 100 + i * 2));
        }
        for (int i = 1; i <= 2; i++) {
            spots.add(new ParkingSpotEntity("F3-L" + i, 3, 8 + i, ParkingSpotType.LARGE, 110 + i * 2));
        }

        floorRepository.saveAll(floors);
        spotRepository.saveAll(spots);

        availabilityService.initializeAvailability();
        log.info("Successfully initialized Parking Lot with {} floors and {} total spots.",
                floors.size(), spots.size());
    }
}
