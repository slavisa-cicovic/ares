package com.sc.ares.common.api;

public class ApiBadRequestException extends RuntimeException {

    public ApiBadRequestException(String message) {
        super(message);
    }
}
