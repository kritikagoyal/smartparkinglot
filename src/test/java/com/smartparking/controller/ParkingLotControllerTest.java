package com.smartparking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartparking.dto.CheckInRequest;
import com.smartparking.dto.CheckInResponse;
import com.smartparking.dto.CheckOutRequest;
import com.smartparking.model.entity.ParkingSpotEntity;
import com.smartparking.model.enums.VehicleType;
import com.smartparking.repository.ParkingSpotRepository;
import com.smartparking.service.AvailabilityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ParkingLotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ParkingSpotRepository spotRepository;

    @Autowired
    private AvailabilityService availabilityService;

    @Autowired
    private com.smartparking.service.ParkingLotService parkingLotService;

    @Autowired
    private com.smartparking.strategy.allocation.NearestSpotAllocationStrategy nearestStrategy;

    @Autowired
    private com.smartparking.strategy.fee.TieredDurationFeeStrategy tieredFeeStrategy;

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
    @DisplayName("REST API: End-to-end check-in, availability check, and checkout")
    void testRestApiFullCycle() throws Exception {
        // 1. Initial Availability
        mockMvc.perform(get("/api/v1/parking/availability"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAvailableSpots", is(30)))
                .andExpect(jsonPath("$.floorAvailabilities", hasSize(3)));

        // 2. Check-In via REST POST /api/v1/parking/entry
        CheckInRequest inReq = new CheckInRequest("DL-08-XY-9999", VehicleType.CAR, "GATE-IN-A");
        String inJson = objectMapper.writeValueAsString(inReq);

        String responseBody = mockMvc.perform(post("/api/v1/parking/entry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(inJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticketId", notNullValue()))
                .andExpect(jsonPath("$.licensePlate", is("DL-08-XY-9999")))
                .andExpect(jsonPath("$.vehicleType", is("CAR")))
                .andExpect(jsonPath("$.assignedSpotId", notNullValue()))
                .andReturn().getResponse().getContentAsString();

        CheckInResponse inResp = objectMapper.readValue(responseBody, CheckInResponse.class);

        // 3. Availability should have decreased to 29
        mockMvc.perform(get("/api/v1/parking/availability"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAvailableSpots", is(29)));

        // 4. Ticket lookup
        mockMvc.perform(get("/api/v1/parking/ticket/" + inResp.ticketId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(inResp.ticketId())))
                .andExpect(jsonPath("$.licensePlate", is("DL-08-XY-9999")));

        // 5. Check-Out via REST POST /api/v1/parking/exit
        CheckOutRequest outReq = new CheckOutRequest(
                inResp.ticketId(),
                "GATE-OUT-B",
                LocalDateTime.now().plusHours(1).plusMinutes(30)
        );
        String outJson = objectMapper.writeValueAsString(outReq);

        mockMvc.perform(post("/api/v1/parking/exit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(outJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceId", notNullValue()))
                .andExpect(jsonPath("$.ticketId", is(inResp.ticketId())))
                .andExpect(jsonPath("$.billedHours", is(2)))
                .andExpect(jsonPath("$.totalFee", is(7.50)))
                .andExpect(jsonPath("$.paymentStatus", is("PAID")));

        // 6. Availability restored to 30
        mockMvc.perform(get("/api/v1/parking/availability"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAvailableSpots", is(30)));
    }

    @Test
    @DisplayName("REST API: Validation error on missing fields")
    void testValidationError() throws Exception {
        // Missing license plate
        String invalidJson = """
                {
                    "vehicleType": "CAR"
                }
                """;

        mockMvc.perform(post("/api/v1/parking/entry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")));
    }

    @Test
    @DisplayName("REST API: Cancel ticket endpoint releases spot and updates availability")
    void testCancelTicketViaApi() throws Exception {
        CheckInRequest inReq = new CheckInRequest("AP-09-CAN-1234", VehicleType.CAR, "GATE-IN-A");
        String responseBody = mockMvc.perform(post("/api/v1/parking/entry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        CheckInResponse inResp = objectMapper.readValue(responseBody, CheckInResponse.class);

        // Cancel ticket
        mockMvc.perform(post("/api/v1/parking/ticket/" + inResp.ticketId() + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CANCELLED")));

        // Availability must be 30
        mockMvc.perform(get("/api/v1/parking/availability"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAvailableSpots", is(30)));
    }

    @Test
    @DisplayName("REST API: Switch allocation and fee strategies at runtime")
    void testSwitchStrategiesViaApi() throws Exception {
        mockMvc.perform(post("/api/v1/parking/strategy/allocation?strategy=bestfit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeStrategy", is("BestFitAllocationStrategy")));

        mockMvc.perform(post("/api/v1/parking/strategy/fee?strategy=flat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeStrategy", is("FlatFeeStrategy")));
    }
}
