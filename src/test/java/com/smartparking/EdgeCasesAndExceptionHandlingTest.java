package com.smartparking;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartparking.dto.CheckInRequest;
import com.smartparking.dto.CheckInResponse;
import com.smartparking.dto.CheckOutRequest;
import com.smartparking.exception.InvalidOperationException;
import com.smartparking.exception.InvalidTicketException;
import com.smartparking.model.domain.Bus;
import com.smartparking.model.domain.Car;
import com.smartparking.model.domain.EntryGate;
import com.smartparking.model.domain.Motorcycle;
import com.smartparking.model.entity.ParkingSpotEntity;
import com.smartparking.model.enums.ParkingSpotType;
import com.smartparking.model.enums.VehicleType;
import com.smartparking.repository.ParkingSpotRepository;
import com.smartparking.service.AvailabilityService;
import com.smartparking.service.ParkingLotService;
import com.smartparking.strategy.allocation.BestFitAllocationStrategy;
import com.smartparking.strategy.allocation.NearestSpotAllocationStrategy;
import com.smartparking.strategy.fee.FeeBreakdown;
import com.smartparking.strategy.fee.FlatFeeStrategy;
import com.smartparking.strategy.fee.TieredDurationFeeStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EdgeCasesAndExceptionHandlingTest {

    @Autowired
    private ParkingLotService parkingLotService;

    @Autowired
    private ParkingSpotRepository spotRepository;

    @Autowired
    private AvailabilityService availabilityService;

    @Autowired
    private NearestSpotAllocationStrategy nearestStrategy;

    @Autowired
    private BestFitAllocationStrategy bestFitStrategy;

    @Autowired
    private TieredDurationFeeStrategy tieredFeeStrategy;

    @Autowired
    private FlatFeeStrategy flatFeeStrategy;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        parkingLotService.setAllocationStrategy(nearestStrategy);
        parkingLotService.setFeeCalculationStrategy(tieredFeeStrategy);
        for (ParkingSpotEntity spot : spotRepository.findAll()) {
            spot.vacate();
            spotRepository.save(spot);
        }
        availabilityService.initializeAvailability();
    }

    @Test
    @DisplayName("Edge Case: Defensive parameter validation on null and blank service arguments")
    void testDefensiveNullValidations() {
        // checkIn validations
        assertThrows(IllegalArgumentException.class, () -> parkingLotService.checkIn(null));
        assertThrows(IllegalArgumentException.class, () -> parkingLotService.checkIn(new CheckInRequest(null, VehicleType.CAR, "G1")));
        assertThrows(IllegalArgumentException.class, () -> parkingLotService.checkIn(new CheckInRequest("   ", VehicleType.CAR, "G1")));
        assertThrows(IllegalArgumentException.class, () -> parkingLotService.checkIn(new CheckInRequest("CAR-1", null, "G1")));

        // checkOut validations
        assertThrows(IllegalArgumentException.class, () -> parkingLotService.checkOut(null));
        assertThrows(IllegalArgumentException.class, () -> parkingLotService.checkOut(new CheckOutRequest(null, "G1", null)));
        assertThrows(IllegalArgumentException.class, () -> parkingLotService.checkOut(new CheckOutRequest("   ", "G1", null)));

        // cancelTicket validations
        assertThrows(IllegalArgumentException.class, () -> parkingLotService.cancelTicket(null));
        assertThrows(IllegalArgumentException.class, () -> parkingLotService.cancelTicket("   "));

        // strategy null checks
        assertThrows(NullPointerException.class, () -> parkingLotService.setAllocationStrategy(null));
        assertThrows(NullPointerException.class, () -> parkingLotService.setFeeCalculationStrategy(null));
    }

    @Test
    @DisplayName("Edge Case: Exact grace period and hourly fee boundaries")
    void testFeeCalculationBoundaries() {
        LocalDateTime entryTime = LocalDateTime.of(2026, 10, 23, 8, 0, 0);
        var ticket = new com.smartparking.model.entity.ParkingTicketEntity(
                "T-FEE", "CAR-FEE", VehicleType.CAR, "F1-C1", 1, "G1", entryTime);

        // Boundary 1: Exactly 0 minutes
        FeeBreakdown fee0 = tieredFeeStrategy.calculateFee(ticket, entryTime);
        assertEquals(new BigDecimal("0.00"), fee0.totalFee());

        // Boundary 2: Exactly 15 minutes (Grace period threshold)
        FeeBreakdown fee15 = tieredFeeStrategy.calculateFee(ticket, entryTime.plusMinutes(15));
        assertEquals(new BigDecimal("0.00"), fee15.totalFee());

        // Boundary 3: Exactly 16 minutes (1 min above grace -> bills 1st hour base fee)
        FeeBreakdown fee16 = tieredFeeStrategy.calculateFee(ticket, entryTime.plusMinutes(16));
        assertEquals(1, fee16.billedHours());
        assertEquals(new BigDecimal("4.00"), fee16.totalFee());

        // Boundary 4: Exactly 60 minutes
        FeeBreakdown fee60 = tieredFeeStrategy.calculateFee(ticket, entryTime.plusMinutes(60));
        assertEquals(1, fee60.billedHours());
        assertEquals(new BigDecimal("4.00"), fee60.totalFee());

        // Boundary 5: Exactly 61 minutes (Ceils to 2 hours -> $4.00 base + $3.50 hourly = $7.50)
        FeeBreakdown fee61 = tieredFeeStrategy.calculateFee(ticket, entryTime.plusMinutes(61));
        assertEquals(2, fee61.billedHours());
        assertEquals(new BigDecimal("7.50"), fee61.totalFee());

        // Boundary 6: 24-hour long stay for a Bus (2.5x multiplier)
        // Hour 1: $4.00, Hours 2-4 (3 hrs @ $3.50): $10.50, Hours 5-24 (20 hrs @ $2.50): $50.00 -> Subtotal $64.50
        // Bus total = $64.50 * 2.5 = $161.25
        var busTicket = new com.smartparking.model.entity.ParkingTicketEntity(
                "T-BUS-24", "BUS-24", VehicleType.BUS, "F1-L1", 1, "G1", entryTime);
        FeeBreakdown fee24h = tieredFeeStrategy.calculateFee(busTicket, entryTime.plusHours(24));
        assertEquals(24, fee24h.billedHours());
        assertEquals(new BigDecimal("161.25"), fee24h.totalFee());
    }

    @Test
    @DisplayName("Edge Case: Allocation strategy null, empty, and incompatible candidate lists")
    void testAllocationEdgeCases() {
        EntryGate gate = new EntryGate("G1", "North");

        // Null and empty lists
        assertTrue(nearestStrategy.allocateSpot(null, new Car("C1"), gate).isEmpty());
        assertTrue(nearestStrategy.allocateSpot(Collections.emptyList(), new Car("C1"), gate).isEmpty());
        assertTrue(nearestStrategy.allocateSpot(List.of(new ParkingSpotEntity("S1", 1, 1, ParkingSpotType.COMPACT, 1)), null, gate).isEmpty());

        // Incompatible candidates: Bus with only Compact and Motorcycle spots
        List<ParkingSpotEntity> nonLargeSpots = List.of(
                new ParkingSpotEntity("S-M", 1, 1, ParkingSpotType.MOTORCYCLE, 5),
                new ParkingSpotEntity("S-C", 1, 2, ParkingSpotType.COMPACT, 10)
        );
        assertTrue(nearestStrategy.allocateSpot(nonLargeSpots, new Bus("B1"), gate).isEmpty());

        // Incompatible candidates: Car with only Motorcycle spots
        List<ParkingSpotEntity> motoOnly = List.of(
                new ParkingSpotEntity("S-M", 1, 1, ParkingSpotType.MOTORCYCLE, 5)
        );
        assertTrue(nearestStrategy.allocateSpot(motoOnly, new Car("C1"), gate).isEmpty());

        // Motorcycle allowed 2-level fallback to Large spot
        List<ParkingSpotEntity> largeOnly = List.of(
                new ParkingSpotEntity("S-L", 1, 1, ParkingSpotType.LARGE, 15)
        );
        var motoAlloc = nearestStrategy.allocateSpot(largeOnly, new Motorcycle("M1"), gate);
        assertTrue(motoAlloc.isPresent());
        assertEquals("S-L", motoAlloc.get().getId());
    }

    @Test
    @DisplayName("Edge Case: Vehicle re-entry after completing check-out is permitted")
    void testReEntryAfterCheckout() {
        String plate = "RE-ENTRY-101";

        // 1st entry
        CheckInResponse in1 = parkingLotService.checkIn(new CheckInRequest(plate, VehicleType.CAR, "G1"));
        assertNotNull(in1.ticketId());

        // 1st exit
        parkingLotService.checkOut(new CheckOutRequest(in1.ticketId(), "G-OUT", in1.entryTime().plusHours(1)));

        // 2nd entry (same vehicle returns later)
        CheckInResponse in2 = parkingLotService.checkIn(new CheckInRequest(plate, VehicleType.CAR, "G1"));
        assertNotNull(in2.ticketId());
        assertNotEquals(in1.ticketId(), in2.ticketId());
    }

    @Test
    @DisplayName("Edge Case: FlatFeeStrategy argument validations")
    void testFlatFeeStrategyValidations() {
        assertThrows(IllegalArgumentException.class, () -> flatFeeStrategy.calculateFee(null, LocalDateTime.now()));

        var ticket = new com.smartparking.model.entity.ParkingTicketEntity(
                "T-1", "C-1", VehicleType.CAR, "S1", 1, "G1", LocalDateTime.now());
        assertThrows(IllegalArgumentException.class, () -> flatFeeStrategy.calculateFee(ticket, null));
        assertThrows(IllegalArgumentException.class, () -> flatFeeStrategy.calculateFee(ticket, LocalDateTime.now().minusHours(1)));
    }

    @Test
    @DisplayName("Edge Case: REST API invalid inputs and malformed payload handling")
    void testRestApiEdgeCases() throws Exception {
        // Unknown strategy query parameters
        mockMvc.perform(post("/api/v1/parking/strategy/allocation?strategy=superfast"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        mockMvc.perform(post("/api/v1/parking/strategy/fee?strategy=unknownfee"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        // Malformed vehicle type enum
        String malformedJson = """
                {
                    "licensePlate": "INVALID-TYPE",
                    "vehicleType": "SPACESHIP"
                }
                """;
        mockMvc.perform(post("/api/v1/parking/entry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }
}
