package com.healthcare.appointmentservice.healthcare.appointmentservice.exception;


public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}