package com.smartparking.repository;

import com.smartparking.model.entity.ParkingFloorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParkingFloorRepository extends JpaRepository<ParkingFloorEntity, String> {
    List<ParkingFloorEntity> findAllByOrderByFloorNumberAsc();
    Optional<ParkingFloorEntity> findByFloorNumber(int floorNumber);
}
