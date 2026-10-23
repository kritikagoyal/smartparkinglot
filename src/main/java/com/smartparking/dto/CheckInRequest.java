package com.smartparking.dto;

import com.smartparking.model.enums.VehicleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CheckInRequest(
        @NotBlank(message = "License plate is required")
        String licensePlate,

        @NotNull(message = "Vehicle type is required (MOTORCYCLE, CAR, BUS)")
        VehicleType vehicleType,

        String entryGateId
) {}
