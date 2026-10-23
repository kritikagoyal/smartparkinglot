package com.smartparking.strategy.fee;

import com.smartparking.model.entity.ParkingTicketEntity;
import com.smartparking.model.enums.VehicleType;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Tiered pricing strategy considering parking duration and vehicle type:
 * - Grace period: <= 15 minutes is free
 * - Hour 1 (Base): $4.00
 * - Hours 2-4: $3.50 / hour
 * - Hours 5+: $2.50 / hour
 * Multipliers:
 * - MOTORCYCLE: 0.6x
 * - CAR: 1.0x
 * - BUS: 2.5x
 */
@Component
@Primary
public class TieredDurationFeeStrategy implements FeeCalculationStrategy {

    public static final long GRACE_PERIOD_MINUTES = 15;
    public static final BigDecimal BASE_FIRST_HOUR_FEE = new BigDecimal("4.00");
    public static final BigDecimal TIER1_HOURLY_RATE = new BigDecimal("3.50"); // Hours 2 to 4
    public static final BigDecimal TIER2_HOURLY_RATE = new BigDecimal("2.50"); // Hours 5+

    @Override
    public FeeBreakdown calculateFee(ParkingTicketEntity ticket, LocalDateTime exitTime) {
        if (ticket == null || exitTime == null) {
            throw new IllegalArgumentException("Ticket and exitTime must not be null");
        }

        LocalDateTime entryTime = ticket.getEntryTime();
        if (exitTime.isBefore(entryTime)) {
            throw new IllegalArgumentException("Exit time cannot be earlier than entry time");
        }

        long durationMinutes = Duration.between(entryTime, exitTime).toMinutes();

        BigDecimal multiplier = getVehicleMultiplier(ticket.getVehicleType());

        // Grace period check
        if (durationMinutes <= GRACE_PERIOD_MINUTES) {
            return new FeeBreakdown(
                    durationMinutes,
                    0,
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    multiplier,
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
            );
        }

        // Billed hours: ceiling of minutes / 60
        long billedHours = (durationMinutes + 59) / 60;
        billedHours = Math.max(1, billedHours);

        BigDecimal baseFee = BASE_FIRST_HOUR_FEE;
        BigDecimal hourlyFee = BigDecimal.ZERO;

        if (billedHours > 1) {
            long remainingHours = billedHours - 1;

            // Tier 1 hours (hours 2, 3, 4 -> up to 3 hours)
            long tier1Hours = Math.min(remainingHours, 3);
            hourlyFee = hourlyFee.add(TIER1_HOURLY_RATE.multiply(BigDecimal.valueOf(tier1Hours)));

            // Tier 2 hours (hours 5+)
            if (remainingHours > 3) {
                long tier2Hours = remainingHours - 3;
                hourlyFee = hourlyFee.add(TIER2_HOURLY_RATE.multiply(BigDecimal.valueOf(tier2Hours)));
            }
        }

        BigDecimal subtotal = baseFee.add(hourlyFee);
        BigDecimal total = subtotal.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);

        return new FeeBreakdown(
                durationMinutes,
                billedHours,
                baseFee.setScale(2, RoundingMode.HALF_UP),
                hourlyFee.setScale(2, RoundingMode.HALF_UP),
                multiplier,
                total
        );
    }

    public BigDecimal getVehicleMultiplier(VehicleType vehicleType) {
        return switch (vehicleType) {
            case MOTORCYCLE -> new BigDecimal("0.60");
            case CAR -> new BigDecimal("1.00");
            case BUS -> new BigDecimal("2.50");
        };
    }
}
