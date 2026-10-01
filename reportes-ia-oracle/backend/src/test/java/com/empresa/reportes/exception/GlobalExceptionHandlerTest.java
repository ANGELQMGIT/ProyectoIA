package com.empresa.reportes.exception;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalExceptionHandlerTest {

    @Test
    void handleAiProvider_ShouldLogStackWithoutRawProviderMessage() {
        Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        MDC.put("correlationId", "trace-test-123");

        try {
            GlobalExceptionHandler handler = new GlobalExceptionHandler();
            handler.handleAiProvider(new AiProviderException(
                    AiProviderException.Category.PROVIDER_FAILURE,
                    "No se pudo generar la consulta con el proveedor de IA",
                    new IllegalStateException("raw provider response and secret token")
            ));

            String loggedStack = appender.list.stream()
                    .map(event -> event.getFormattedMessage() + " " + ThrowableProxyUtil.asString(event.getThrowableProxy()))
                    .reduce("", (all, line) -> all + line);

            assertTrue(loggedStack.contains("trace-test-123"));
            assertTrue(loggedStack.contains("IllegalStateException"));
            assertTrue(loggedStack.contains("[detalle omitido]"));
            assertFalse(loggedStack.contains("raw provider response"));
            assertFalse(loggedStack.contains("secret token"));
        } finally {
            MDC.clear();
            logger.detachAppender(appender);
            appender.stop();
        }
    }
}