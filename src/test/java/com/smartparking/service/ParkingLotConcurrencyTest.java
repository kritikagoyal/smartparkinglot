package com.smartparking.service;

import com.smartparking.dto.AvailabilityResponse;
import com.smartparking.dto.CheckInRequest;
import com.smartparking.dto.CheckInResponse;
import com.smartparking.dto.CheckOutRequest;
import com.smartparking.dto.CheckOutResponse;
import com.smartparking.exception.ParkingFullException;
import com.smartparking.model.entity.ParkingSpotEntity;
import com.smartparking.model.enums.ParkingSpotStatus;
import com.smartparking.model.enums.VehicleType;
import com.smartparking.repository.ParkingSpotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ParkingLotConcurrencyTest {

    @Autowired
    private ParkingLotService parkingLotService;

    @Autowired
    private AvailabilityService availabilityService;

    @Autowired
    private ParkingSpotRepository spotRepository;

    @BeforeEach
    void setUp() {
        for (ParkingSpotEntity spot : spotRepository.findAll()) {
            spot.vacate();
            spotRepository.save(spot);
        }
        availabilityService.initializeAvailability();
    }

    @Test
    @DisplayName("50 concurrent car entry requests: exactly 22 spots allocated, zero duplicate spot assignments, remaining 28 rejected")
    void testConcurrentEntriesWithZeroDoubleBooking() throws InterruptedException {
        int totalThreads = 50;
        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(totalThreads);

        List<CheckInResponse> successfulEntries = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger rejectedEntries = new AtomicInteger(0);
        AtomicInteger unexpectedErrors = new AtomicInteger(0);

        for (int i = 0; i < totalThreads; i++) {
            final String plate = "RACE-CAR-" + String.format("%03d", i);
            executor.submit(() -> {
                try {
                    startGate.await(); // Wait for all threads to align at the exact same instant
                    CheckInResponse response = parkingLotService.checkIn(
                            new CheckInRequest(plate, VehicleType.CAR, "GATE-NORTH-1")
                    );
                    successfulEntries.add(response);
                } catch (ParkingFullException e) {
                    rejectedEntries.incrementAndGet();
                } catch (Exception e) {
                    unexpectedErrors.incrementAndGet();
                } finally {
                    endGate.countDown();
                }
            });
        }

        // Release the latch to start all 50 threads simultaneously
        startGate.countDown();
        boolean completedInTime = endGate.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completedInTime, "All concurrent tasks should complete within timeout");
        assertEquals(0, unexpectedErrors.get(), "There should be no unexpected errors");

        // There are 22 spots that can fit a car in the parking lot (16 Compact + 6 Large)
        assertEquals(22, successfulEntries.size(), "Exactly 22 cars should be successfully allocated a spot");
        assertEquals(28, rejectedEntries.get(), "Remaining 28 cars should be rejected as parking is full for cars");

        // CRITICAL INVARIANT: Zero double-allocations! Every allocated spot ID must be completely unique.
        Set<String> allocatedSpotIds = new HashSet<>();
        for (CheckInResponse entry : successfulEntries) {
            boolean isUnique = allocatedSpotIds.add(entry.assignedSpotId());
            assertTrue(isUnique, "Spot " + entry.assignedSpotId() + " was assigned to multiple vehicles!");
        }
        assertEquals(22, allocatedSpotIds.size(), "All 22 assigned spots must be distinct");

        // Verify database state: exactly 22 spots occupied, 8 available (the 8 motorcycle spots)
        long dbOccupiedCount = spotRepository.findAll().stream()
                .filter(s -> s.getStatus() == ParkingSpotStatus.OCCUPIED)
                .count();
        assertEquals(22, dbOccupiedCount, "Database occupied count must exactly match 22");

        // Verify real-time availability cache
        AvailabilityResponse avail = availabilityService.getAvailability();
        assertEquals(8, avail.totalAvailableSpots(), "Only 8 spots (motorcycle spots) should remain available");

        // NOW TEST CONCURRENT CHECK-OUT: All 22 parked cars exit simultaneously
        ExecutorService checkoutExecutor = Executors.newFixedThreadPool(22);
        CountDownLatch checkoutStart = new CountDownLatch(1);
        CountDownLatch checkoutEnd = new CountDownLatch(22);
        List<CheckOutResponse> successfulExits = Collections.synchronizedList(new ArrayList<>());

        for (CheckInResponse entry : successfulEntries) {
            checkoutExecutor.submit(() -> {
                try {
                    checkoutStart.await();
                    CheckOutResponse out = parkingLotService.checkOut(new CheckOutRequest(
                            entry.ticketId(),
                            "GATE-SOUTH-1",
                            LocalDateTime.now().plusHours(2)
                    ));
                    successfulExits.add(out);
                } catch (Exception e) {
                    unexpectedErrors.incrementAndGet();
                } finally {
                    checkoutEnd.countDown();
                }
            });
        }

        checkoutStart.countDown();
        boolean checkoutCompleted = checkoutEnd.await(15, TimeUnit.SECONDS);
        checkoutExecutor.shutdown();

        assertTrue(checkoutCompleted, "Concurrent checkout should complete within timeout");
        assertEquals(22, successfulExits.size(), "All 22 cars should successfully check out");

        // Verify lot is now completely free (30 spots available)
        AvailabilityResponse afterAllExits = availabilityService.getAvailability();
        assertEquals(30, afterAllExits.totalAvailableSpots(), "All 30 spots must be available after checkout");
    }
}
