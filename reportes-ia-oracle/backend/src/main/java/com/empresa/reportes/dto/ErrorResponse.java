package com.empresa.reportes.dto;

public record ErrorResponse(
        String error,
        String mensaje,
        int status,
        String traceId
) {
}
