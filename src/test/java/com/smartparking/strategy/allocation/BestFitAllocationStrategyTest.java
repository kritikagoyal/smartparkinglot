package com.smartparking.strategy.allocation;

import com.smartparking.model.domain.Car;
import com.smartparking.model.domain.EntryGate;
import com.smartparking.model.domain.Motorcycle;
import com.smartparking.model.domain.Vehicle;
import com.smartparking.model.entity.ParkingSpotEntity;
import com.smartparking.model.enums.ParkingSpotType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class BestFitAllocationStrategyTest {

    private BestFitAllocationStrategy strategy;
    private EntryGate entryGate;

    @BeforeEach
    void setUp() {
        strategy = new BestFitAllocationStrategy();
        entryGate = new EntryGate("GATE-1", "Main Entrance");
    }

    @Test
    @DisplayName("Best-Fit should strictly allocate exact match (Compact) for Car even if Large spot is closer")
    void testBestFitChoosesExactFitOverCloserLargerSpot() {
        List<ParkingSpotEntity> spots = List.of(
                new ParkingSpotEntity("F1-L1", 1, 1, ParkingSpotType.LARGE, 5),   // Closer Large spot
                new ParkingSpotEntity("F1-C1", 1, 2, ParkingSpotType.COMPACT, 25) // Farther Compact spot
        );

        Vehicle car = new Car("CAR-BEST-1");
        Optional<ParkingSpotEntity> allocated = strategy.allocateSpot(spots, car, entryGate);

        assertTrue(allocated.isPresent());
        // BestFit prioritizes fit tier 0 (Compact) over fit tier 1 (Large)
        assertEquals("F1-C1", allocated.get().getId());
    }

    @Test
    @DisplayName("Best-Fit should allocate Motorcycle spot for Motorcycle")
    void testBestFitMotorcycle() {
        List<ParkingSpotEntity> spots = List.of(
                new ParkingSpotEntity("F1-C1", 1, 1, ParkingSpotType.COMPACT, 10),
                new ParkingSpotEntity("F1-M1", 1, 2, ParkingSpotType.MOTORCYCLE, 20)
        );

        Vehicle moto = new Motorcycle("MOTO-BEST-1");
        Optional<ParkingSpotEntity> allocated = strategy.allocateSpot(spots, moto, entryGate);

        assertTrue(allocated.isPresent());
        assertEquals("F1-M1", allocated.get().getId());
    }
}
