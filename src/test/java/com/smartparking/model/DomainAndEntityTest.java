package com.smartparking.model;

import com.smartparking.exception.GlobalExceptionHandler;
import com.smartparking.exception.ParkingLotException;
import com.smartparking.model.domain.*;
import com.smartparking.model.entity.*;
import com.smartparking.model.enums.ParkingSpotStatus;
import com.smartparking.model.enums.ParkingSpotType;
import com.smartparking.model.enums.VehicleType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class DomainAndEntityTest {

    @Test
    @DisplayName("Vehicle domain model: equality, hashCode, toString, and validation")
    void testVehicleDomain() {
        Vehicle car1 = new Car("KA-01-1234");
        Vehicle car2 = new Car("ka-01-1234");
        Vehicle moto = new Motorcycle("KA-01-1234");
        Vehicle bus = new Bus("KA-01-9999");

        assertEquals(car1, car2);
        assertEquals(car1.hashCode(), car2.hashCode());
        assertNotEquals(car1, moto);
        assertNotEquals(car1, null);
        assertNotEquals(car1, "Some String");

        assertTrue(car1.toString().contains("KA-01-1234"));
        assertEquals(VehicleType.BUS, bus.getVehicleType());

        assertThrows(IllegalArgumentException.class, () -> new Car(""));
        assertThrows(IllegalArgumentException.class, () -> new Car(null));
    }

    @Test
    @DisplayName("VehicleFactory: instantiates correct sub-classes and rejects nulls")
    void testVehicleFactory() {
        Vehicle v1 = VehicleFactory.createVehicle("P1", VehicleType.MOTORCYCLE);
        assertTrue(v1 instanceof Motorcycle);

        Vehicle v2 = VehicleFactory.createVehicle("P2", VehicleType.CAR);
        assertTrue(v2 instanceof Car);

        Vehicle v3 = VehicleFactory.createVehicle("P3", VehicleType.BUS);
        assertTrue(v3 instanceof Bus);

        assertThrows(IllegalArgumentException.class, () -> VehicleFactory.createVehicle("P4", null));
    }

    @Test
    @DisplayName("Gates domain: EntryGate and ExitGate")
    void testGates() {
        EntryGate entry = new EntryGate("G1", "North");
        ExitGate exit = new ExitGate("G2", "South");

        assertEquals("G1", entry.getId());
        assertEquals("North", entry.getName());
        assertEquals("G2", exit.getId());
        assertEquals("South", exit.getName());
    }

    @Test
    @DisplayName("ParkingSpotEntity: state transitions, equals, hashCode, toString")
    void testParkingSpotEntity() {
        ParkingSpotEntity spot = new ParkingSpotEntity("S-1", 1, 1, ParkingSpotType.COMPACT, 10);
        assertTrue(spot.isAvailable());

        spot.occupy("PLATE-1");
        assertFalse(spot.isAvailable());
        assertEquals("PLATE-1", spot.getCurrentVehiclePlate());

        assertThrows(IllegalStateException.class, () -> spot.occupy("PLATE-2"));

        spot.vacate();
        assertTrue(spot.isAvailable());
        assertNull(spot.getCurrentVehiclePlate());

        ParkingSpotEntity spotCopy = new ParkingSpotEntity("S-1", 1, 1, ParkingSpotType.COMPACT, 10);
        assertEquals(spot, spotCopy);
        assertEquals(spot.hashCode(), spotCopy.hashCode());

        assertNotNull(spot.toString());

        spot.setVersion(1L);
        assertEquals(1L, spot.getVersion());
    }

    @Test
    @DisplayName("ParkingFloorEntity and ParkingLotEntity: getters and setters")
    void testFloorAndLotEntities() {
        ParkingLotEntity lot = new ParkingLotEntity("L1", "Main Lot", "123 St", 2);
        lot.setId("L2");
        lot.setName("New Lot");
        lot.setAddress("456 Ave");
        lot.setTotalFloors(3);

        assertEquals("L2", lot.getId());
        assertEquals("New Lot", lot.getName());
        assertEquals("456 Ave", lot.getAddress());
        assertEquals(3, lot.getTotalFloors());

        ParkingFloorEntity floor = new ParkingFloorEntity("FL-1", "L2", 1, 10);
        floor.setId("FL-2");
        floor.setLotId("L3");
        floor.setFloorNumber(2);
        floor.setTotalSpots(20);

        assertEquals("FL-2", floor.getId());
        assertEquals("L3", floor.getLotId());
        assertEquals(2, floor.getFloorNumber());
        assertEquals(20, floor.getTotalSpots());
    }

    @Test
    @DisplayName("GlobalExceptionHandler: covers general exception and illegal arguments")
    void testGlobalExceptionHandler() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        var res1 = handler.handleIllegalArgument(new IllegalArgumentException("Invalid argument test"));
        assertEquals(HttpStatus.BAD_REQUEST, res1.getStatusCode());
        assertEquals("BAD_REQUEST", res1.getBody().error());

        var res2 = handler.handleGeneralException(new RuntimeException("Unexpected error"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, res2.getStatusCode());
        assertEquals("INTERNAL_SERVER_ERROR", res2.getBody().error());

        ParkingLotException ex = new ParkingLotException("Custom error", new Throwable("Cause"));
        assertEquals("Custom error", ex.getMessage());
        assertNotNull(ex.getCause());

        var res3 = handler.handleParkingFull(new com.smartparking.exception.ParkingFullException("Full"));
        assertEquals(HttpStatus.CONFLICT, res3.getStatusCode());

        var res4 = handler.handleVehicleAlreadyParked(new com.smartparking.exception.VehicleAlreadyParkedException("Already"));
        assertEquals(HttpStatus.CONFLICT, res4.getStatusCode());

        var res5 = handler.handleInvalidTicket(new com.smartparking.exception.InvalidTicketException("Invalid"));
        assertEquals(HttpStatus.NOT_FOUND, res5.getStatusCode());

        var res6 = handler.handleInvalidOperation(new com.smartparking.exception.InvalidOperationException("Op"));
        assertEquals(HttpStatus.CONFLICT, res6.getStatusCode());
    }

    @Test
    @DisplayName("InvoiceEntity and ParkingTicketEntity: getters, setters and methods")
    void testInvoiceAndTicketEntities() {
        LocalDateTime now = LocalDateTime.now();
        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setId("INV-1");
        invoice.setTicketId("TKT-1");
        invoice.setLicensePlate("CAR-1");
        invoice.setVehicleType(VehicleType.CAR);
        invoice.setSpotId("S-1");
        invoice.setExitGateId("G-OUT");
        invoice.setEntryTime(now.minusHours(1));
        invoice.setExitTime(now);
        invoice.setDurationMinutes(60);
        invoice.setBaseFee(new java.math.BigDecimal("4.00"));
        invoice.setHourlyFee(java.math.BigDecimal.ZERO);
        invoice.setVehicleMultiplier(java.math.BigDecimal.ONE);
        invoice.setTotalAmount(new java.math.BigDecimal("4.00"));
        invoice.setPaymentStatus(com.smartparking.model.enums.PaymentStatus.PAID);
        invoice.setCreatedAt(now);

        assertEquals("INV-1", invoice.getId());
        assertEquals("TKT-1", invoice.getTicketId());
        assertEquals("CAR-1", invoice.getLicensePlate());
        assertEquals(VehicleType.CAR, invoice.getVehicleType());
        assertEquals("S-1", invoice.getSpotId());
        assertEquals("G-OUT", invoice.getExitGateId());
        assertEquals(60, invoice.getDurationMinutes());
        assertEquals(new java.math.BigDecimal("4.00"), invoice.getBaseFee());
        assertEquals(java.math.BigDecimal.ZERO, invoice.getHourlyFee());
        assertEquals(java.math.BigDecimal.ONE, invoice.getVehicleMultiplier());
        assertEquals(new java.math.BigDecimal("4.00"), invoice.getTotalAmount());
        assertEquals(com.smartparking.model.enums.PaymentStatus.PAID, invoice.getPaymentStatus());
        assertEquals(now, invoice.getCreatedAt());

        ParkingTicketEntity ticket = new ParkingTicketEntity();
        ticket.setId("T-1");
        ticket.setLicensePlate("PLATE-X");
        ticket.setVehicleType(VehicleType.BUS);
        ticket.setSpotId("L-1");
        ticket.setFloorNumber(1);
        ticket.setEntryGateId("G-IN");
        ticket.setEntryTime(now);
        ticket.setExitTime(now.plusHours(2));
        ticket.setStatus(com.smartparking.model.enums.TicketStatus.COMPLETED);

        assertEquals("T-1", ticket.getId());
        assertEquals("PLATE-X", ticket.getLicensePlate());
        assertEquals(VehicleType.BUS, ticket.getVehicleType());
        assertEquals("L-1", ticket.getSpotId());
        assertEquals(1, ticket.getFloorNumber());
        assertEquals("G-IN", ticket.getEntryGateId());
        assertEquals(now, ticket.getEntryTime());
        assertEquals(now.plusHours(2), ticket.getExitTime());
        assertEquals(com.smartparking.model.enums.TicketStatus.COMPLETED, ticket.getStatus());
    }
}
