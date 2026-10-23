package com.smartparking.strategy.allocation;

import com.smartparking.model.domain.*;
import com.smartparking.model.entity.ParkingSpotEntity;
import com.smartparking.model.enums.ParkingSpotType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class NearestSpotAllocationStrategyTest {

    private NearestSpotAllocationStrategy strategy;
    private EntryGate entryGate;

    @BeforeEach
    void setUp() {
        strategy = new NearestSpotAllocationStrategy();
        entryGate = new EntryGate("GATE-1", "Main Entrance");
    }

    @Test
    @DisplayName("Motorcycle should prefer Motorcycle spot over Compact or Large")
    void testMotorcyclePrefersMotorcycleSpot() {
        List<ParkingSpotEntity> spots = List.of(
                new ParkingSpotEntity("F1-L1", 1, 1, ParkingSpotType.LARGE, 10),
                new ParkingSpotEntity("F1-C1", 1, 2, ParkingSpotType.COMPACT, 15),
                new ParkingSpotEntity("F1-M1", 1, 3, ParkingSpotType.MOTORCYCLE, 20)
        );

        Vehicle moto = new Motorcycle("MOTO-101");
        Optional<ParkingSpotEntity> allocated = strategy.allocateSpot(spots, moto, entryGate);

        assertTrue(allocated.isPresent());
        assertEquals("F1-M1", allocated.get().getId());
        assertEquals(ParkingSpotType.MOTORCYCLE, allocated.get().getSpotType());
    }

    @Test
    @DisplayName("Motorcycle should fallback to Compact when no Motorcycle spot available")
    void testMotorcycleFallbackToCompact() {
        List<ParkingSpotEntity> spots = List.of(
                new ParkingSpotEntity("F1-L1", 1, 1, ParkingSpotType.LARGE, 10),
                new ParkingSpotEntity("F1-C1", 1, 2, ParkingSpotType.COMPACT, 15)
        );

        Vehicle moto = new Motorcycle("MOTO-102");
        Optional<ParkingSpotEntity> allocated = strategy.allocateSpot(spots, moto, entryGate);

        assertTrue(allocated.isPresent());
        assertEquals("F1-C1", allocated.get().getId());
        assertEquals(ParkingSpotType.COMPACT, allocated.get().getSpotType());
    }

    @Test
    @DisplayName("Motorcycle should fallback to Large when only Large spot is available")
    void testMotorcycleFallbackToLarge() {
        List<ParkingSpotEntity> spots = List.of(
                new ParkingSpotEntity("F1-L1", 1, 1, ParkingSpotType.LARGE, 10)
        );

        Vehicle moto = new Motorcycle("MOTO-103");
        Optional<ParkingSpotEntity> allocated = strategy.allocateSpot(spots, moto, entryGate);

        assertTrue(allocated.isPresent());
        assertEquals("F1-L1", allocated.get().getId());
    }

    @Test
    @DisplayName("Car should prefer Compact spot over Large spot")
    void testCarPrefersCompact() {
        List<ParkingSpotEntity> spots = List.of(
                new ParkingSpotEntity("F1-M1", 1, 1, ParkingSpotType.MOTORCYCLE, 5),
                new ParkingSpotEntity("F1-L1", 1, 2, ParkingSpotType.LARGE, 10),
                new ParkingSpotEntity("F1-C1", 1, 3, ParkingSpotType.COMPACT, 15)
        );

        Vehicle car = new Car("CAR-201");
        Optional<ParkingSpotEntity> allocated = strategy.allocateSpot(spots, car, entryGate);

        assertTrue(allocated.isPresent());
        assertEquals("F1-C1", allocated.get().getId());
        assertEquals(ParkingSpotType.COMPACT, allocated.get().getSpotType());
    }

    @Test
    @DisplayName("Car should fallback to Large when Compact is unavailable")
    void testCarFallbackToLarge() {
        List<ParkingSpotEntity> spots = List.of(
                new ParkingSpotEntity("F1-M1", 1, 1, ParkingSpotType.MOTORCYCLE, 5),
                new ParkingSpotEntity("F1-L1", 1, 2, ParkingSpotType.LARGE, 10)
        );

        Vehicle car = new Car("CAR-202");
        Optional<ParkingSpotEntity> allocated = strategy.allocateSpot(spots, car, entryGate);

        assertTrue(allocated.isPresent());
        assertEquals("F1-L1", allocated.get().getId());
    }

    @Test
    @DisplayName("Bus should ONLY be assigned to Large spot")
    void testBusOnlyAssignedToLarge() {
        List<ParkingSpotEntity> spotsWithoutLarge = List.of(
                new ParkingSpotEntity("F1-M1", 1, 1, ParkingSpotType.MOTORCYCLE, 5),
                new ParkingSpotEntity("F1-C1", 1, 2, ParkingSpotType.COMPACT, 10)
        );

        Vehicle bus = new Bus("BUS-301");
        Optional<ParkingSpotEntity> allocated = strategy.allocateSpot(spotsWithoutLarge, bus, entryGate);

        assertTrue(allocated.isEmpty(), "Bus must not be assigned to motorcycle or compact spot");

        List<ParkingSpotEntity> spotsWithLarge = new ArrayList<>(spotsWithoutLarge);
        spotsWithLarge.add(new ParkingSpotEntity("F2-L1", 2, 1, ParkingSpotType.LARGE, 25));

        Optional<ParkingSpotEntity> allocatedWithLarge = strategy.allocateSpot(spotsWithLarge, bus, entryGate);
        assertTrue(allocatedWithLarge.isPresent());
        assertEquals("F2-L1", allocatedWithLarge.get().getId());
    }

    @Test
    @DisplayName("Within same spot type, should select lower floor first, then closest distance")
    void testProximityOrdering() {
        List<ParkingSpotEntity> spots = List.of(
                new ParkingSpotEntity("F2-C1", 2, 1, ParkingSpotType.COMPACT, 5),   // Floor 2, dist 5
                new ParkingSpotEntity("F1-C2", 1, 2, ParkingSpotType.COMPACT, 30),  // Floor 1, dist 30
                new ParkingSpotEntity("F1-C1", 1, 1, ParkingSpotType.COMPACT, 10)   // Floor 1, dist 10 (Closest on Floor 1)
        );

        Vehicle car = new Car("CAR-203");
        Optional<ParkingSpotEntity> allocated = strategy.allocateSpot(spots, car, entryGate);

        assertTrue(allocated.isPresent());
        // Should choose F1-C1 because Floor 1 is lower than Floor 2, and among Floor 1, dist 10 < 30
        assertEquals("F1-C1", allocated.get().getId());
    }
}
