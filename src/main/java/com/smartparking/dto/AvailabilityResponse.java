package com.smartparking.dto;

import java.util.List;

public record AvailabilityResponse(
        String lotId,
        String lotName,
        long totalAvailableSpots,
        long totalCapacity,
        List<FloorAvailabilityDto> floorAvailabilities
) {}
