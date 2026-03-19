package com.sc.ares.common.api;

public record ApiErrorResponse(
    String timestamp,
    int status,
    String error,
    String message,
    String path
) {
}
