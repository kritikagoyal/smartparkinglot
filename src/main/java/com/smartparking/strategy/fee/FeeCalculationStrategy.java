package com.smartparking.strategy.fee;

import com.smartparking.model.entity.ParkingTicketEntity;

import java.time.LocalDateTime;

public interface FeeCalculationStrategy {
    /**
     * Calculates the fee breakdown for a given ticket and exit timestamp.
     *
     * @param ticket active parking ticket
     * @param exitTime vehicle departure timestamp
     * @return fee breakdown including duration and total fee
     */
    FeeBreakdown calculateFee(ParkingTicketEntity ticket, LocalDateTime exitTime);
}
