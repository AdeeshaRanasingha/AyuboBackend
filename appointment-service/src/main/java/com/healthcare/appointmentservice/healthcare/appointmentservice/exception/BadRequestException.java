package com.healthcare.appointmentservice.healthcare.appointmentservice.exception;


public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}