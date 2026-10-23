package com.smartparking.dto;

import com.smartparking.model.enums.VehicleType;

import java.time.LocalDateTime;

public record CheckInResponse(
        String ticketId,
        String licensePlate,
        VehicleType vehicleType,
        String assignedSpotId,
        int floorNumber,
        String entryGateId,
        LocalDateTime entryTime,
        String message
) {}
