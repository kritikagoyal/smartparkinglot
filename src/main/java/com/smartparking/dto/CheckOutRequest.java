package com.smartparking.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public record CheckOutRequest(
        @NotBlank(message = "Ticket ID is required")
        String ticketId,

        String exitGateId,

        /**
         * Optional exit timestamp (for time simulation / testing). If null, defaults to now.
         */
        LocalDateTime exitTime
) {}
