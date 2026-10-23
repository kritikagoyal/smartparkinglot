package com.smartparking.strategy.fee;

import com.smartparking.model.entity.ParkingTicketEntity;
import com.smartparking.model.enums.VehicleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FlatFeeStrategyTest {

    private FlatFeeStrategy flatStrategy;
    private LocalDateTime baseTime;

    @BeforeEach
    void setUp() {
        flatStrategy = new FlatFeeStrategy();
        baseTime = LocalDateTime.of(2026, 10, 23, 10, 0, 0);
    }

    @Test
    @DisplayName("Flat rate: 2 hours for Car should be $10.00 (2 hrs * $5.00 * 1.0x)")
    void testCarFlatRate() {
        ParkingTicketEntity ticket = new ParkingTicketEntity(
                "TKT-FLAT-1", "CAR-FLAT-1", VehicleType.CAR, "F1-C1", 1, "GATE-1", baseTime);
        FeeBreakdown fee = flatStrategy.calculateFee(ticket, baseTime.plusHours(2));

        assertEquals(2, fee.billedHours());
        assertEquals(new BigDecimal("10.00"), fee.totalFee());
    }

    @Test
    @DisplayName("Flat rate: 2 hours for Motorcycle should be $5.00 (2 hrs * $5.00 * 0.5x)")
    void testMotorcycleFlatRate() {
        ParkingTicketEntity ticket = new ParkingTicketEntity(
                "TKT-FLAT-2", "MOTO-FLAT-1", VehicleType.MOTORCYCLE, "F1-M1", 1, "GATE-1", baseTime);
        FeeBreakdown fee = flatStrategy.calculateFee(ticket, baseTime.plusHours(2));

        assertEquals(2, fee.billedHours());
        assertEquals(new BigDecimal("5.00"), fee.totalFee());
    }
}
