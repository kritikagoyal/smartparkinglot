package com.smartparking.repository;

import com.smartparking.model.entity.ParkingTicketEntity;
import com.smartparking.model.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ParkingTicketRepository extends JpaRepository<ParkingTicketEntity, String> {
    Optional<ParkingTicketEntity> findByIdAndStatus(String id, TicketStatus status);
    Optional<ParkingTicketEntity> findByLicensePlateAndStatus(String licensePlate, TicketStatus status);
}
