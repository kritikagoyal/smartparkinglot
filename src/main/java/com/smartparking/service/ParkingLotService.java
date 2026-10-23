package com.smartparking.service;

import com.smartparking.dto.CheckInRequest;
import com.smartparking.dto.CheckInResponse;
import com.smartparking.dto.CheckOutRequest;
import com.smartparking.dto.CheckOutResponse;
import com.smartparking.exception.InvalidOperationException;
import com.smartparking.exception.InvalidTicketException;
import com.smartparking.exception.ParkingFullException;
import com.smartparking.exception.VehicleAlreadyParkedException;
import com.smartparking.model.domain.EntryGate;
import com.smartparking.model.domain.Vehicle;
import com.smartparking.model.domain.VehicleFactory;
import com.smartparking.model.entity.InvoiceEntity;
import com.smartparking.model.entity.ParkingSpotEntity;
import com.smartparking.model.entity.ParkingTicketEntity;
import com.smartparking.model.enums.ParkingSpotStatus;
import com.smartparking.model.enums.PaymentStatus;
import com.smartparking.model.enums.TicketStatus;
import com.smartparking.model.event.SpotStatusChangedEvent;
import com.smartparking.repository.InvoiceRepository;
import com.smartparking.repository.ParkingSpotRepository;
import com.smartparking.repository.ParkingTicketRepository;
import com.smartparking.strategy.allocation.SpotAllocationStrategy;
import com.smartparking.strategy.fee.FeeBreakdown;
import com.smartparking.strategy.fee.FeeCalculationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class ParkingLotService {

    private static final Logger log = LoggerFactory.getLogger(ParkingLotService.class);
    private static final int MAX_ALLOCATION_RETRIES = 5;

    private final ParkingSpotRepository spotRepository;
    private final ParkingTicketRepository ticketRepository;
    private final InvoiceRepository invoiceRepository;
    private final SpotAllocationService spotAllocationService;
    private final FeeCalculationService feeCalculationService;
    private final ApplicationEventPublisher eventPublisher;

    public ParkingLotService(ParkingSpotRepository spotRepository,
                              ParkingTicketRepository ticketRepository,
                              InvoiceRepository invoiceRepository,
                              SpotAllocationService spotAllocationService,
                              FeeCalculationService feeCalculationService,
                              ApplicationEventPublisher eventPublisher) {
        this.spotRepository = Objects.requireNonNull(spotRepository, "ParkingSpotRepository must not be null");
        this.ticketRepository = Objects.requireNonNull(ticketRepository, "ParkingTicketRepository must not be null");
        this.invoiceRepository = Objects.requireNonNull(invoiceRepository, "InvoiceRepository must not be null");
        this.spotAllocationService = Objects.requireNonNull(spotAllocationService, "SpotAllocationService must not be null");
        this.feeCalculationService = Objects.requireNonNull(feeCalculationService, "FeeCalculationService must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "ApplicationEventPublisher must not be null");
    }

    /**
     * Handles vehicle check-in: allocates spot, issues ticket, and updates real-time state.
     * Includes concurrency retry protection against simultaneous gate claims.
     */
    public CheckInResponse checkIn(CheckInRequest request) {
        if (request == null || request.licensePlate() == null || request.licensePlate().isBlank()) {
            throw new IllegalArgumentException("License plate must not be null or blank");
        }
        if (request.vehicleType() == null) {
            throw new IllegalArgumentException("Vehicle type must not be null");
        }

        String licensePlate = request.licensePlate().trim().toUpperCase();

        // 1. Check if vehicle is already parked with active ticket
        Optional<ParkingTicketEntity> existingTicket = ticketRepository
                .findByLicensePlateAndStatus(licensePlate, TicketStatus.ACTIVE);
        if (existingTicket.isPresent()) {
            throw new VehicleAlreadyParkedException("Vehicle " + licensePlate +
                    " is already parked inside with active ticket: " + existingTicket.get().getId());
        }

        Vehicle vehicle = VehicleFactory.createVehicle(licensePlate, request.vehicleType());
        String gateId = request.entryGateId() != null && !request.entryGateId().isBlank()
                ? request.entryGateId() : "GATE-IN-1";
        EntryGate gate = new EntryGate(gateId, "Gate " + gateId);

        // 2. Concurrency-safe spot allocation with retry
        ParkingSpotEntity assignedSpot = null;
        for (int attempt = 1; attempt <= MAX_ALLOCATION_RETRIES; attempt++) {
            Optional<ParkingSpotEntity> candidateOpt = spotAllocationService.findBestSpot(vehicle, gate);
            if (candidateOpt.isEmpty()) {
                throw new ParkingFullException("Parking lot is full for vehicle type: " + request.vehicleType());
            }

            ParkingSpotEntity candidate = candidateOpt.get();
            ReentrantLock spotLock = spotAllocationService.getLockForSpot(candidate.getId());
            spotLock.lock();
            try {
                // Re-verify freshness from database inside spot lock
                Optional<ParkingSpotEntity> freshCandidateOpt = spotRepository.findByIdAndStatus(candidate.getId(), ParkingSpotStatus.AVAILABLE);
                if (freshCandidateOpt.isPresent()) {
                    ParkingSpotEntity spot = freshCandidateOpt.get();
                    spot.occupy(licensePlate);
                    assignedSpot = spotRepository.saveAndFlush(spot);
                    log.info("Assigned spot {} on floor {} to vehicle {} (attempt {})",
                            assignedSpot.getId(), assignedSpot.getFloorNumber(), licensePlate, attempt);
                    break;
                }
            } catch (ObjectOptimisticLockingFailureException e) {
                log.warn("Optimistic lock conflict on spot {} during check-in attempt {}. Retrying...",
                        candidate.getId(), attempt);
            } finally {
                spotLock.unlock();
            }
        }

        if (assignedSpot == null) {
            throw new ParkingFullException("Could not allocate spot due to concurrent load. Please retry.");
        }

        // 3. Issue parking ticket
        LocalDateTime entryTime = LocalDateTime.now();
        String ticketId = "TKT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        ParkingTicketEntity ticket = new ParkingTicketEntity(
                ticketId,
                licensePlate,
                vehicle.getVehicleType(),
                assignedSpot.getId(),
                assignedSpot.getFloorNumber(),
                gate.getId(),
                entryTime
        );
        ticketRepository.save(ticket);

        // 4. Notify observers of real-time spot change
        eventPublisher.publishEvent(new SpotStatusChangedEvent(
                assignedSpot.getId(),
                assignedSpot.getFloorNumber(),
                assignedSpot.getSpotType(),
                ParkingSpotStatus.AVAILABLE,
                ParkingSpotStatus.OCCUPIED
        ));

        return new CheckInResponse(
                ticket.getId(),
                ticket.getLicensePlate(),
                ticket.getVehicleType(),
                assignedSpot.getId(),
                assignedSpot.getFloorNumber(),
                ticket.getEntryGateId(),
                ticket.getEntryTime(),
                "Vehicle checked in successfully. Proceed to Floor " + assignedSpot.getFloorNumber() +
                        ", Spot " + assignedSpot.getId()
        );
    }

    /**
     * Handles vehicle check-out: calculates fees, releases spot, and marks ticket completed.
     */
    @Transactional
    public CheckOutResponse checkOut(CheckOutRequest request) {
        if (request == null || request.ticketId() == null || request.ticketId().isBlank()) {
            throw new IllegalArgumentException("Ticket ID must not be null or blank");
        }

        // 1. Fetch ticket and validate state
        ParkingTicketEntity ticket = ticketRepository.findById(request.ticketId().trim())
                .orElseThrow(() -> new InvalidTicketException("No ticket found for ID: " + request.ticketId()));

        if (ticket.getStatus() == TicketStatus.COMPLETED) {
            throw new InvalidOperationException("Ticket " + request.ticketId() + " has already been paid and completed.");
        }
        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new InvalidOperationException("Cannot checkout ticket " + request.ticketId() + " because it was cancelled.");
        }

        LocalDateTime exitTime = request.exitTime() != null ? request.exitTime() : LocalDateTime.now();
        String exitGateId = request.exitGateId() != null && !request.exitGateId().isBlank()
                ? request.exitGateId() : "GATE-OUT-1";

        // 2. Fee Calculation
        FeeBreakdown fee = feeCalculationService.calculateFee(ticket, exitTime);

        // 3. Free the parking spot
        ReentrantLock spotLock = spotAllocationService.getLockForSpot(ticket.getSpotId());
        spotLock.lock();
        ParkingSpotEntity spot;
        try {
            spot = spotRepository.findById(ticket.getSpotId())
                    .orElseThrow(() -> new IllegalStateException("Associated spot " + ticket.getSpotId() + " not found"));
            spot.vacate();
            spotRepository.saveAndFlush(spot);
        } finally {
            spotLock.unlock();
        }

        // 4. Update ticket status
        ticket.complete(exitTime);
        ticketRepository.save(ticket);

        // 5. Generate and persist Invoice
        String invoiceId = "INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        InvoiceEntity invoice = new InvoiceEntity(
                invoiceId,
                ticket.getId(),
                ticket.getLicensePlate(),
                ticket.getVehicleType(),
                spot.getId(),
                exitGateId,
                ticket.getEntryTime(),
                exitTime,
                fee.durationMinutes(),
                fee.baseFee(),
                fee.hourlyFee(),
                fee.vehicleMultiplier(),
                fee.totalFee(),
                PaymentStatus.PAID
        );
        invoiceRepository.save(invoice);

        // 6. Notify observers of spot availability increase
        eventPublisher.publishEvent(new SpotStatusChangedEvent(
                spot.getId(),
                spot.getFloorNumber(),
                spot.getSpotType(),
                ParkingSpotStatus.OCCUPIED,
                ParkingSpotStatus.AVAILABLE
        ));

        log.info("Vehicle {} checked out via gate {}. Duration: {} min. Fee: ${}",
                ticket.getLicensePlate(), exitGateId, fee.durationMinutes(), fee.totalFee());

        return new CheckOutResponse(
                invoice.getId(),
                ticket.getId(),
                ticket.getLicensePlate(),
                ticket.getVehicleType(),
                spot.getId(),
                exitGateId,
                ticket.getEntryTime(),
                exitTime,
                fee.durationMinutes(),
                fee.billedHours(),
                fee.baseFee(),
                fee.hourlyFee(),
                fee.vehicleMultiplier(),
                fee.totalFee(),
                invoice.getPaymentStatus(),
                "Checkout completed successfully. Have a safe drive!"
        );
    }

    /**
     * Cancels an active parking ticket and frees the allocated spot.
     * Strictly idempotent: rejects already completed or cancelled tickets.
     */
    @Transactional
    public void cancelTicket(String ticketId) {
        if (ticketId == null || ticketId.isBlank()) {
            throw new IllegalArgumentException("Ticket ID must not be null or blank");
        }

        ParkingTicketEntity ticket = ticketRepository.findById(ticketId.trim())
                .orElseThrow(() -> new InvalidTicketException("No ticket found for ID: " + ticketId));

        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new InvalidOperationException("Ticket " + ticketId + " has already been cancelled.");
        }
        if (ticket.getStatus() == TicketStatus.COMPLETED) {
            throw new InvalidOperationException("Cannot cancel ticket " + ticketId + " because it is already completed / paid.");
        }

        // Free the parking spot
        ReentrantLock spotLock = spotAllocationService.getLockForSpot(ticket.getSpotId());
        spotLock.lock();
        ParkingSpotEntity spot;
        try {
            spot = spotRepository.findById(ticket.getSpotId())
                    .orElseThrow(() -> new IllegalStateException("Associated spot " + ticket.getSpotId() + " not found"));
            spot.vacate();
            spotRepository.saveAndFlush(spot);
        } finally {
            spotLock.unlock();
        }

        ticket.cancel();
        ticketRepository.save(ticket);

        eventPublisher.publishEvent(new SpotStatusChangedEvent(
                spot.getId(),
                spot.getFloorNumber(),
                spot.getSpotType(),
                ParkingSpotStatus.OCCUPIED,
                ParkingSpotStatus.AVAILABLE
        ));

        log.info("Ticket {} for vehicle {} successfully cancelled. Spot {} vacated.",
                ticket.getId(), ticket.getLicensePlate(), spot.getId());
    }

    public void setAllocationStrategy(SpotAllocationStrategy strategy) {
        spotAllocationService.setAllocationStrategy(strategy);
    }

    public SpotAllocationStrategy getAllocationStrategy() {
        return spotAllocationService.getAllocationStrategy();
    }

    public void setFeeCalculationStrategy(FeeCalculationStrategy strategy) {
        feeCalculationService.setFeeCalculationStrategy(strategy);
    }

    public FeeCalculationStrategy getFeeCalculationStrategy() {
        return feeCalculationService.getFeeCalculationStrategy();
    }

    public Optional<ParkingTicketEntity> getActiveTicket(String ticketId) {
        return ticketRepository.findById(ticketId);
    }

    public Optional<ParkingTicketEntity> getActiveTicketByPlate(String licensePlate) {
        return ticketRepository.findByLicensePlateAndStatus(licensePlate.trim().toUpperCase(), TicketStatus.ACTIVE);
    }
}
