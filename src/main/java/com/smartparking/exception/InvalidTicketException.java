package com.smartparking.exception;

public class InvalidTicketException extends ParkingLotException {
    public InvalidTicketException(String message) {
        super(message);
    }
}
