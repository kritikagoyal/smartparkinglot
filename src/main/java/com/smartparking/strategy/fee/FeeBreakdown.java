package com.smartparking.strategy.fee;

import java.math.BigDecimal;

public record FeeBreakdown(
        long durationMinutes,
        long billedHours,
        BigDecimal baseFee,
        BigDecimal hourlyFee,
        BigDecimal vehicleMultiplier,
        BigDecimal totalFee
) {}
