package com.smartparking.dto;

public record FloorAvailabilityDto(
        int floorNumber,
        long availableMotorcycleSpots,
        long availableCompactSpots,
        long availableLargeSpots,
        long totalAvailableSpots,
        long totalCapacity
) {}
