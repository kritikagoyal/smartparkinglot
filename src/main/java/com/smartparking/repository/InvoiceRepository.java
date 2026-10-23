package com.smartparking.repository;

import com.smartparking.model.entity.InvoiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<InvoiceEntity, String> {
    Optional<InvoiceEntity> findByTicketId(String ticketId);
    List<InvoiceEntity> findByLicensePlateOrderByCreatedAtDesc(String licensePlate);
}
