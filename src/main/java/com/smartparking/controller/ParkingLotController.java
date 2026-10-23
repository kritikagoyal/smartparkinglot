package com.smartparking.controller;

import com.smartparking.dto.AvailabilityResponse;
import com.smartparking.dto.CheckInRequest;
import com.smartparking.dto.CheckInResponse;
import com.smartparking.dto.CheckOutRequest;
import com.smartparking.dto.CheckOutResponse;
import com.smartparking.exception.InvalidTicketException;
import com.smartparking.model.entity.ParkingTicketEntity;
import com.smartparking.service.AvailabilityService;
import com.smartparking.service.ParkingLotService;
import com.smartparking.strategy.allocation.BestFitAllocationStrategy;
import com.smartparking.strategy.allocation.NearestSpotAllocationStrategy;
import com.smartparking.strategy.fee.FlatFeeStrategy;
import com.smartparking.strategy.fee.TieredDurationFeeStrategy;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/parking")
public class ParkingLotController {

    private final ParkingLotService parkingLotService;
    private final AvailabilityService availabilityService;
    private final NearestSpotAllocationStrategy nearestStrategy;
    private final BestFitAllocationStrategy bestFitStrategy;
    private final TieredDurationFeeStrategy tieredFeeStrategy;
    private final FlatFeeStrategy flatFeeStrategy;

    public ParkingLotController(ParkingLotService parkingLotService,
                                 AvailabilityService availabilityService,
                                 NearestSpotAllocationStrategy nearestStrategy,
                                 BestFitAllocationStrategy bestFitStrategy,
                                 TieredDurationFeeStrategy tieredFeeStrategy,
                                 FlatFeeStrategy flatFeeStrategy) {
        this.parkingLotService = parkingLotService;
        this.availabilityService = availabilityService;
        this.nearestStrategy = nearestStrategy;
        this.bestFitStrategy = bestFitStrategy;
        this.tieredFeeStrategy = tieredFeeStrategy;
        this.flatFeeStrategy = flatFeeStrategy;
    }

    /**
     * Check-in endpoint: automatically allocates spot based on vehicle size and returns ticket.
     */
    @PostMapping("/entry")
    public ResponseEntity<CheckInResponse> checkIn(@Valid @RequestBody CheckInRequest request) {
        CheckInResponse response = parkingLotService.checkIn(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Check-out endpoint: calculates fee based on stay duration and vehicle type, frees spot.
     */
    @PostMapping("/exit")
    public ResponseEntity<CheckOutResponse> checkOut(@Valid @RequestBody CheckOutRequest request) {
        CheckOutResponse response = parkingLotService.checkOut(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Real-time availability endpoint: returns available spots breakdown per floor and type.
     */
    @GetMapping("/availability")
    public ResponseEntity<AvailabilityResponse> getAvailability() {
        AvailabilityResponse response = availabilityService.getAvailability();
        return ResponseEntity.ok(response);
    }

    /**
     * Ticket cancellation endpoint.
     */
    @PostMapping("/ticket/{ticketId}/cancel")
    public ResponseEntity<Map<String, String>> cancelTicket(@PathVariable("ticketId") String ticketId) {
        parkingLotService.cancelTicket(ticketId);
        return ResponseEntity.ok(Map.of(
                "ticketId", ticketId,
                "status", "CANCELLED",
                "message", "Ticket cancelled successfully and spot vacated"
        ));
    }

    /**
     * Runtime allocation strategy switcher.
     */
    @PostMapping("/strategy/allocation")
    public ResponseEntity<Map<String, String>> switchAllocationStrategy(@RequestParam("strategy") String strategyName) {
        if ("bestfit".equalsIgnoreCase(strategyName)) {
            parkingLotService.setAllocationStrategy(bestFitStrategy);
        } else if ("nearest".equalsIgnoreCase(strategyName)) {
            parkingLotService.setAllocationStrategy(nearestStrategy);
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "Unknown strategy: " + strategyName + ". Supported: 'nearest', 'bestfit'"));
        }
        return ResponseEntity.ok(Map.of(
                "activeStrategy", parkingLotService.getAllocationStrategy().getClass().getSimpleName(),
                "message", "Allocation strategy successfully switched"
        ));
    }

    /**
     * Runtime fee strategy switcher.
     */
    @PostMapping("/strategy/fee")
    public ResponseEntity<Map<String, String>> switchFeeStrategy(@RequestParam("strategy") String strategyName) {
        if ("flat".equalsIgnoreCase(strategyName)) {
            parkingLotService.setFeeCalculationStrategy(flatFeeStrategy);
        } else if ("tiered".equalsIgnoreCase(strategyName)) {
            parkingLotService.setFeeCalculationStrategy(tieredFeeStrategy);
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "Unknown strategy: " + strategyName + ". Supported: 'tiered', 'flat'"));
        }
        return ResponseEntity.ok(Map.of(
                "activeStrategy", parkingLotService.getFeeCalculationStrategy().getClass().getSimpleName(),
                "message", "Fee calculation strategy successfully switched"
        ));
    }

    /**
     * Ticket lookup endpoint.
     */
    @GetMapping("/ticket/{ticketId}")
    public ResponseEntity<ParkingTicketEntity> getTicket(@PathVariable("ticketId") String ticketId) {
        ParkingTicketEntity ticket = parkingLotService.getActiveTicket(ticketId)
                .orElseThrow(() -> new InvalidTicketException("Ticket with ID " + ticketId + " not found"));
        return ResponseEntity.ok(ticket);
    }
}
