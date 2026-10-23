package com.smartparking.service;

import com.smartparking.dto.AvailabilityResponse;
import com.smartparking.dto.CheckInRequest;
import com.smartparking.dto.CheckInResponse;
import com.smartparking.dto.CheckOutRequest;
import com.smartparking.dto.CheckOutResponse;
import com.smartparking.exception.InvalidOperationException;
import com.smartparking.exception.InvalidTicketException;
import com.smartparking.exception.VehicleAlreadyParkedException;
import com.smartparking.strategy.allocation.BestFitAllocationStrategy;
import com.smartparking.strategy.allocation.NearestSpotAllocationStrategy;
import com.smartparking.strategy.fee.TieredDurationFeeStrategy;
import com.smartparking.model.entity.ParkingSpotEntity;
import com.smartparking.model.enums.ParkingSpotStatus;
import com.smartparking.model.enums.PaymentStatus;
import com.smartparking.model.enums.VehicleType;
import com.smartparking.repository.ParkingSpotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ParkingLotServiceIntegrationTest {

    @Autowired
    private ParkingLotService parkingLotService;

    @Autowired
    private AvailabilityService availabilityService;

    @Autowired
    private ParkingSpotRepository spotRepository;

    @Autowired
    private InitDataService initDataService;

    @Autowired
    private NearestSpotAllocationStrategy nearestStrategy;

    @Autowired
    private TieredDurationFeeStrategy tieredFeeStrategy;

    @BeforeEach
    void setUp() {
        parkingLotService.setAllocationStrategy(nearestStrategy);
        parkingLotService.setFeeCalculationStrategy(tieredFeeStrategy);
        // Ensure standard state
        for (ParkingSpotEntity spot : spotRepository.findAll()) {
            spot.vacate();
            spotRepository.save(spot);
        }
        availabilityService.initializeAvailability();
    }

    @Test
    @DisplayName("End-to-end Check-In and Check-Out with real-time availability update and fee calculation")
    void testEndToEndCheckInAndCheckOut() {
        // 1. Initial Availability
        AvailabilityResponse initialAvailability = availabilityService.getAvailability();
        assertEquals(30, initialAvailability.totalAvailableSpots());
        assertEquals(30, initialAvailability.totalCapacity());

        // 2. Check-In Car
        CheckInRequest entryRequest = new CheckInRequest("KA-01-AB-1234", VehicleType.CAR, "GATE-NORTH-1");
        CheckInResponse entryResponse = parkingLotService.checkIn(entryRequest);

        assertNotNull(entryResponse.ticketId());
        assertEquals("KA-01-AB-1234", entryResponse.licensePlate());
        assertEquals(VehicleType.CAR, entryResponse.vehicleType());
        assertNotNull(entryResponse.assignedSpotId());

        // Verify spot is OCCUPIED in database
        ParkingSpotEntity spot = spotRepository.findById(entryResponse.assignedSpotId()).orElseThrow();
        assertEquals(ParkingSpotStatus.OCCUPIED, spot.getStatus());
        assertEquals("KA-01-AB-1234", spot.getCurrentVehiclePlate());

        // 3. Real-time availability must decrease by 1
        AvailabilityResponse afterEntry = availabilityService.getAvailability();
        assertEquals(29, afterEntry.totalAvailableSpots());

        // 4. Duplicate Check-In with same plate should fail
        assertThrows(VehicleAlreadyParkedException.class, () ->
                parkingLotService.checkIn(entryRequest));

        // 5. Check-Out after 2.5 hours
        LocalDateTime exitTime = entryResponse.entryTime().plusHours(2).plusMinutes(30);
        CheckOutRequest exitRequest = new CheckOutRequest(
                entryResponse.ticketId(),
                "GATE-SOUTH-1",
                exitTime
        );
        CheckOutResponse exitResponse = parkingLotService.checkOut(exitRequest);

        assertNotNull(exitResponse.invoiceId());
        assertEquals(entryResponse.ticketId(), exitResponse.ticketId());
        assertEquals(PaymentStatus.PAID, exitResponse.paymentStatus());
        assertEquals(150, exitResponse.durationMinutes());
        assertEquals(3, exitResponse.billedHours()); // 150 mins -> 3 hours
        assertEquals(new BigDecimal("4.00"), exitResponse.baseFee());
        assertEquals(new BigDecimal("7.00"), exitResponse.hourlyFee());
        assertEquals(new BigDecimal("11.00"), exitResponse.totalFee());

        // 6. Spot must be restored to AVAILABLE in database
        ParkingSpotEntity spotAfterExit = spotRepository.findById(entryResponse.assignedSpotId()).orElseThrow();
        assertEquals(ParkingSpotStatus.AVAILABLE, spotAfterExit.getStatus());
        assertNull(spotAfterExit.getCurrentVehiclePlate());

        // 7. Real-time availability must be back to 30
        AvailabilityResponse afterExit = availabilityService.getAvailability();
        assertEquals(30, afterExit.totalAvailableSpots());
    }

    @Test
    @DisplayName("Checking out with invalid ticket ID should throw InvalidTicketException")
    void testInvalidTicketCheckOut() {
        CheckOutRequest request = new CheckOutRequest("INVALID-TICKET-999", "GATE-1", LocalDateTime.now());
        assertThrows(InvalidTicketException.class, () -> parkingLotService.checkOut(request));
    }

    @Test
    @DisplayName("Ticket cancellation should vacate spot and prevent double-cancellation or checkout")
    void testTicketCancellationLifecycle() {
        CheckInRequest entryRequest = new CheckInRequest("MOTO-CAN-001", VehicleType.MOTORCYCLE, "GATE-NORTH-1");
        CheckInResponse entryResponse = parkingLotService.checkIn(entryRequest);

        // Spot occupied, availability decreased
        assertEquals(29, availabilityService.getAvailability().totalAvailableSpots());

        // Cancel ticket
        parkingLotService.cancelTicket(entryResponse.ticketId());

        // Spot must be freed, availability restored
        ParkingSpotEntity spot = spotRepository.findById(entryResponse.assignedSpotId()).orElseThrow();
        assertEquals(ParkingSpotStatus.AVAILABLE, spot.getStatus());
        assertEquals(30, availabilityService.getAvailability().totalAvailableSpots());

        // Repeat cancellation must be rejected with InvalidOperationException
        assertThrows(InvalidOperationException.class, () ->
                parkingLotService.cancelTicket(entryResponse.ticketId()));

        // Checkout on cancelled ticket must be rejected with InvalidOperationException
        assertThrows(InvalidOperationException.class, () ->
                parkingLotService.checkOut(new CheckOutRequest(entryResponse.ticketId(), "GATE-1", LocalDateTime.now())));
    }

    @Test
    @DisplayName("Repeat checkout on already paid ticket should be rejected with InvalidOperationException")
    void testRepeatCheckoutFails() {
        CheckInRequest entryRequest = new CheckInRequest("CAR-REPEAT-001", VehicleType.CAR, "GATE-NORTH-1");
        CheckInResponse entryResponse = parkingLotService.checkIn(entryRequest);

        CheckOutRequest checkout = new CheckOutRequest(entryResponse.ticketId(), "GATE-1", LocalDateTime.now().plusHours(1));
        parkingLotService.checkOut(checkout);

        // Repeat checkout
        assertThrows(InvalidOperationException.class, () -> parkingLotService.checkOut(checkout));
    }

    @Test
    @DisplayName("Should dynamically switch allocation strategy at runtime")
    void testSwitchAllocationStrategyAtRuntime() {
        assertTrue(parkingLotService.getAllocationStrategy() instanceof NearestSpotAllocationStrategy);

        BestFitAllocationStrategy bestFit = new BestFitAllocationStrategy();
        parkingLotService.setAllocationStrategy(bestFit);

        assertSame(bestFit, parkingLotService.getAllocationStrategy());
    }
}
