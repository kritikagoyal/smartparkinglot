package com.smartparking.dto;

import com.smartparking.model.enums.PaymentStatus;
import com.smartparking.model.enums.VehicleType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CheckOutResponse(
        String invoiceId,
        String ticketId,
        String licensePlate,
        VehicleType vehicleType,
        String spotId,
        String exitGateId,
        LocalDateTime entryTime,
        LocalDateTime exitTime,
        long durationMinutes,
        long billedHours,
        BigDecimal baseFee,
        BigDecimal hourlyFee,
        BigDecimal vehicleMultiplier,
        BigDecimal totalFee,
        PaymentStatus paymentStatus,
        String message
) {}
