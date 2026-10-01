package com.empresa.reportes.exception;

public class SqlSecurityViolationException extends RuntimeException {
    public SqlSecurityViolationException(String message) {
        super(message);
    }
}
