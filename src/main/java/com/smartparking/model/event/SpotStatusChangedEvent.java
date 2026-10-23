package com.smartparking.model.event;

import com.smartparking.model.enums.ParkingSpotStatus;
import com.smartparking.model.enums.ParkingSpotType;

public record SpotStatusChangedEvent(
        String spotId,
        int floorNumber,
        ParkingSpotType spotType,
        ParkingSpotStatus oldStatus,
        ParkingSpotStatus newStatus
) {}
