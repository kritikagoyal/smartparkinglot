package com.smartparking.strategy.fee;

import com.smartparking.model.entity.ParkingTicketEntity;
import com.smartparking.model.enums.VehicleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TieredDurationFeeStrategyTest {

    private TieredDurationFeeStrategy feeStrategy;
    private LocalDateTime baseTime;

    @BeforeEach
    void setUp() {
        feeStrategy = new TieredDurationFeeStrategy();
        baseTime = LocalDateTime.of(2026, 10, 23, 10, 0, 0);
    }

    @Test
    @DisplayName("Should charge $0.00 if vehicle departs within 15-minute grace period")
    void testGracePeriod() {
        ParkingTicketEntity ticket = new ParkingTicketEntity(
                "TKT-1", "CAR-101", VehicleType.CAR, "F1-C1", 1, "GATE-1", baseTime);

        // 12 minutes stay
        LocalDateTime exitTime = baseTime.plusMinutes(12);
        FeeBreakdown fee = feeStrategy.calculateFee(ticket, exitTime);

        assertEquals(12, fee.durationMinutes());
        assertEquals(0, fee.billedHours());
        assertEquals(new BigDecimal("0.00"), fee.totalFee());
    }

    @Test
    @DisplayName("Should charge base fee for stays up to 1 hour (above grace period)")
    void testFirstHourBaseFee() {
        ParkingTicketEntity carTicket = new ParkingTicketEntity(
                "TKT-2", "CAR-102", VehicleType.CAR, "F1-C1", 1, "GATE-1", baseTime);

        // 45 minutes stay (Car: 1.0x multiplier)
        LocalDateTime exitTime = baseTime.plusMinutes(45);
        FeeBreakdown fee = feeStrategy.calculateFee(carTicket, exitTime);

        assertEquals(45, fee.durationMinutes());
        assertEquals(1, fee.billedHours());
        assertEquals(new BigDecimal("4.00"), fee.baseFee());
        assertEquals(new BigDecimal("0.00"), fee.hourlyFee());
        assertEquals(new BigDecimal("4.00"), fee.totalFee());
    }

    @Test
    @DisplayName("Should apply vehicle type multipliers correctly")
    void testVehicleMultipliers() {
        // Motorcycle (0.6x): $4.00 * 0.6 = $2.40
        ParkingTicketEntity motoTicket = new ParkingTicketEntity(
                "TKT-MOTO", "MOTO-1", VehicleType.MOTORCYCLE, "F1-M1", 1, "GATE-1", baseTime);
        FeeBreakdown motoFee = feeStrategy.calculateFee(motoTicket, baseTime.plusMinutes(45));
        assertEquals(new BigDecimal("2.40"), motoFee.totalFee());

        // Bus (2.5x): $4.00 * 2.5 = $10.00
        ParkingTicketEntity busTicket = new ParkingTicketEntity(
                "TKT-BUS", "BUS-1", VehicleType.BUS, "F1-L1", 1, "GATE-1", baseTime);
        FeeBreakdown busFee = feeStrategy.calculateFee(busTicket, baseTime.plusMinutes(45));
        assertEquals(new BigDecimal("10.00"), busFee.totalFee());
    }

    @Test
    @DisplayName("Should calculate Tier 1 rate correctly for 3 hours stay")
    void testTier1Pricing() {
        // Car parked for 2 hours 15 minutes -> ceil to 3 billed hours
        // Hour 1: $4.00, Hours 2-3 (2 hrs): 2 * $3.50 = $7.00. Total = $11.00
        ParkingTicketEntity carTicket = new ParkingTicketEntity(
                "TKT-3", "CAR-103", VehicleType.CAR, "F1-C2", 1, "GATE-1", baseTime);
        LocalDateTime exitTime = baseTime.plusHours(2).plusMinutes(15);
        FeeBreakdown fee = feeStrategy.calculateFee(carTicket, exitTime);

        assertEquals(3, fee.billedHours());
        assertEquals(new BigDecimal("4.00"), fee.baseFee());
        assertEquals(new BigDecimal("7.00"), fee.hourlyFee());
        assertEquals(new BigDecimal("11.00"), fee.totalFee());
    }

    @Test
    @DisplayName("Should calculate Tier 2 rate correctly for long stay (6 hours)")
    void testTier2Pricing() {
        // Car parked for 6 hours:
        // Hour 1: $4.00
        // Hours 2,3,4 (3 hrs @ $3.50): $10.50
        // Hours 5,6 (2 hrs @ $2.50): $5.00
        // Total = $4.00 + $10.50 + $5.00 = $19.50
        ParkingTicketEntity carTicket = new ParkingTicketEntity(
                "TKT-4", "CAR-104", VehicleType.CAR, "F1-C3", 1, "GATE-1", baseTime);
        LocalDateTime exitTime = baseTime.plusHours(6);
        FeeBreakdown fee = feeStrategy.calculateFee(carTicket, exitTime);

        assertEquals(6, fee.billedHours());
        assertEquals(new BigDecimal("4.00"), fee.baseFee());
        assertEquals(new BigDecimal("15.50"), fee.hourlyFee());
        assertEquals(new BigDecimal("19.50"), fee.totalFee());
    }

    @Test
    @DisplayName("Should throw exception when exit time is earlier than entry time")
    void testInvalidExitTime() {
        ParkingTicketEntity carTicket = new ParkingTicketEntity(
                "TKT-5", "CAR-105", VehicleType.CAR, "F1-C3", 1, "GATE-1", baseTime);

        assertThrows(IllegalArgumentException.class, () ->
                feeStrategy.calculateFee(carTicket, baseTime.minusMinutes(10)));
    }
}
