package com.smartparking.service;

import com.smartparking.model.entity.ParkingTicketEntity;
import com.smartparking.strategy.fee.FeeBreakdown;
import com.smartparking.strategy.fee.FeeCalculationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class FeeCalculationService {

    private static final Logger log = LoggerFactory.getLogger(FeeCalculationService.class);

    private volatile FeeCalculationStrategy feeCalculationStrategy;

    public FeeCalculationService(FeeCalculationStrategy feeCalculationStrategy) {
        this.feeCalculationStrategy = Objects.requireNonNull(feeCalculationStrategy, "FeeCalculationStrategy must not be null");
    }

    public FeeCalculationStrategy getFeeCalculationStrategy() {
        return feeCalculationStrategy;
    }

    public void setFeeCalculationStrategy(FeeCalculationStrategy strategy) {
        this.feeCalculationStrategy = Objects.requireNonNull(strategy, "FeeCalculationStrategy must not be null");
        log.info("Switched active fee calculation strategy to: {}", strategy.getClass().getSimpleName());
    }

    public FeeBreakdown calculateFee(ParkingTicketEntity ticket, LocalDateTime exitTime) {
        return feeCalculationStrategy.calculateFee(ticket, exitTime);
    }
}
