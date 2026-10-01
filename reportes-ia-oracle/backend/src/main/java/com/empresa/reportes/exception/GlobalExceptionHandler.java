package com.empresa.reportes.exception;

import com.empresa.reportes.dto.ErrorResponse;
import com.empresa.reportes.filter.CorrelationIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLException;
import java.util.IdentityHashMap;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(SqlSecurityViolationException.class)
    public ResponseEntity<ErrorResponse> handleSqlSecurity(SqlSecurityViolationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("SQL_SECURITY_VIOLATION", ex.getMessage(), HttpStatus.BAD_REQUEST.value(), traceId()));
    }

    @ExceptionHandler(ReporteNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ReporteNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse("REPORTE_NOT_FOUND", ex.getMessage(), HttpStatus.NOT_FOUND.value(), traceId()));
    }

    @ExceptionHandler(AiTimeoutException.class)
    public ResponseEntity<ErrorResponse> handleAiTimeout(AiTimeoutException ex) {
        log.error("Fallo de IA provider=Gemini category=TIMEOUT traceId={}", traceId(), sanitizedStack(ex));
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                .body(new ErrorResponse("AI_TIMEOUT", "El proveedor de IA no respondió a tiempo", HttpStatus.GATEWAY_TIMEOUT.value(), traceId()));
    }

    @ExceptionHandler(AiProviderException.class)
    public ResponseEntity<ErrorResponse> handleAiProvider(AiProviderException ex) {
        log.error("Fallo de IA provider=Gemini category={} traceId={}", ex.getCategory(), traceId(), sanitizedStack(ex));
        HttpStatus status = switch (ex.getCategory()) {
            case NOT_CONFIGURED -> HttpStatus.SERVICE_UNAVAILABLE;
            case TIMEOUT -> HttpStatus.GATEWAY_TIMEOUT;
            case PROVIDER_FAILURE, INVALID_RESPONSE -> HttpStatus.BAD_GATEWAY;
        };
        String error = switch (ex.getCategory()) {
            case NOT_CONFIGURED -> "AI_UNAVAILABLE";
            case TIMEOUT -> "AI_TIMEOUT";
            case PROVIDER_FAILURE -> "AI_PROVIDER_ERROR";
            case INVALID_RESPONSE -> "AI_INVALID_RESPONSE";
        };
        String message = switch (ex.getCategory()) {
            case NOT_CONFIGURED -> "El proveedor de IA no está disponible";
            case TIMEOUT -> "El proveedor de IA no respondió a tiempo";
            case PROVIDER_FAILURE -> "No se pudo generar la consulta con el proveedor de IA";
            case INVALID_RESPONSE -> "El proveedor de IA devolvió una respuesta no válida";
        };
        return ResponseEntity.status(status)
                .body(new ErrorResponse(error, message, status.value(), traceId()));
    }

    @ExceptionHandler({DataAccessException.class, SQLException.class})
    public ResponseEntity<ErrorResponse> handleDataAccess(Exception ex) {
        log.error("Error de infraestructura JDBC", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(
                        "INFRASTRUCTURE_ERROR",
                        "No se pudo completar la consulta contra la base de datos",
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        traceId()
                ));
    }

    private String traceId() {
        return MDC.get(CorrelationIdFilter.MDC_KEY);
    }

    private Throwable sanitizedStack(Throwable failure) {
        return sanitizedStack(failure, true, new IdentityHashMap<>());
    }

    private Throwable sanitizedStack(
            Throwable failure,
            boolean includeMessage,
            IdentityHashMap<Throwable, Throwable> sanitized
    ) {
        if (failure == null) {
            return null;
        }
        Throwable existing = sanitized.get(failure);
        if (existing != null) {
            return existing;
        }

        String detail = includeMessage ? failure.getMessage() : "[detalle omitido]";
        Throwable safeFailure = new Throwable(failure.getClass().getName() + ": " + detail);
        safeFailure.setStackTrace(failure.getStackTrace());
        sanitized.put(failure, safeFailure);

        Throwable cause = failure.getCause();
        if (cause != null && cause != failure) {
            safeFailure.initCause(sanitizedStack(cause, false, sanitized));
        }
        for (Throwable suppressed : failure.getSuppressed()) {
            safeFailure.addSuppressed(sanitizedStack(suppressed, false, sanitized));
        }
        return safeFailure;
    }
}
