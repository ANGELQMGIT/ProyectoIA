package com.empresa.reportes.exception;

public class AiProviderException extends RuntimeException {

    public enum Category {
        NOT_CONFIGURED,
        TIMEOUT,
        PROVIDER_FAILURE,
        INVALID_RESPONSE
    }

    private final Category category;

    public AiProviderException(Category category, String message) {
        super(message);
        this.category = category;
    }

    public AiProviderException(Category category, String message, Throwable cause) {
        super(message, cause);
        this.category = category;
    }

    public Category getCategory() {
        return category;
    }
}