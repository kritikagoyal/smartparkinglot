package com.smartparking.strategy.fee;

import com.smartparking.model.entity.ParkingTicketEntity;
import com.smartparking.model.enums.VehicleType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Alternative pricing strategy: Flat rate per hour with vehicle multiplier.
 */
@Component("flatFeeStrategy")
public class FlatFeeStrategy implements FeeCalculationStrategy {

    public static final BigDecimal FLAT_HOURLY_RATE = new BigDecimal("5.00");

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
        long billedHours = Math.max(1, (durationMinutes + 59) / 60);

        BigDecimal multiplier = switch (ticket.getVehicleType()) {
            case MOTORCYCLE -> new BigDecimal("0.50");
            case CAR -> new BigDecimal("1.00");
            case BUS -> new BigDecimal("2.00");
        };

        BigDecimal base = FLAT_HOURLY_RATE.multiply(BigDecimal.valueOf(billedHours));
        BigDecimal total = base.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);

        return new FeeBreakdown(
                durationMinutes,
                billedHours,
                base.setScale(2, RoundingMode.HALF_UP),
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                multiplier,
                total
        );
    }
}
