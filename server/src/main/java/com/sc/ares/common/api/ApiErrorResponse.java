package com.sc.ares.common.api;

import java.time.Instant;

public record ApiErrorResponse(
    String timestamp,
    int status,
    String error,
    String message,
    String path
) {
}
