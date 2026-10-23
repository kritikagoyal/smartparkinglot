package com.smartparking.repository;

import com.smartparking.model.entity.ParkingSpotEntity;
import com.smartparking.model.enums.ParkingSpotStatus;
import com.smartparking.model.enums.ParkingSpotType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParkingSpotRepository extends JpaRepository<ParkingSpotEntity, String> {

    List<ParkingSpotEntity> findByStatus(ParkingSpotStatus status);

    List<ParkingSpotEntity> findByFloorNumber(int floorNumber);

    List<ParkingSpotEntity> findByStatusAndSpotType(ParkingSpotStatus status, ParkingSpotType spotType);

    Optional<ParkingSpotEntity> findByIdAndStatus(String id, ParkingSpotStatus status);

    long countByFloorNumberAndStatus(int floorNumber, ParkingSpotStatus status);

    long countByFloorNumberAndSpotTypeAndStatus(int floorNumber, ParkingSpotType spotType, ParkingSpotStatus status);

    @Query("SELECT s FROM ParkingSpotEntity s WHERE s.status = 'AVAILABLE' ORDER BY s.floorNumber ASC, s.distanceMetric ASC, s.spotNumber ASC")
    List<ParkingSpotEntity> findAllAvailableOrderedByProximity();

    @Query("SELECT s FROM ParkingSpotEntity s WHERE s.status = 'AVAILABLE' AND s.spotType IN :types ORDER BY s.floorNumber ASC, s.distanceMetric ASC, s.spotNumber ASC")
    List<ParkingSpotEntity> findAvailableBySpotTypesOrderedByProximity(@Param("types") List<ParkingSpotType> types);
}
