package com.empresa.reportes.service;

import com.empresa.reportes.exception.AiProviderException;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.SocketTimeoutException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;

class AiSqlGeneratorServiceTest {

    @Test
    @DisplayName("Debe fallar con error de disponibilidad si ChatModel no está configurado")
    void generateSql_ShouldFailWhenChatModelIsMissing() {
        AiSqlGeneratorService generator = new AiSqlGeneratorService((ChatModel) null);

        AiProviderException exception = assertThrows(AiProviderException.class, () -> generator.generateSql(
                "Ventas por Cliente",
                "Instrucción sistema",
                "SELECT * FROM V_VENTAS_DETALLADAS",
                "Filtrar clientes de tipo EMPRESA"
        ));

        assertEquals(AiProviderException.Category.NOT_CONFIGURED, exception.getCategory());
    }

    @Test
    @DisplayName("Debe propagar un error tipado y conservar la causa del proveedor")
    void generateSql_ShouldPreserveProviderFailureCause() {
        ChatModel chatModel = mock(ChatModel.class);
        RuntimeException providerFailure = new RuntimeException("provider detail");
        when(chatModel.call(any(Prompt.class))).thenThrow(providerFailure);
        AiSqlGeneratorService generator = new AiSqlGeneratorService(chatModel);

        AiProviderException exception = assertThrows(AiProviderException.class, () -> generator.generateSql(
                "Ventas por Cliente", "Sistema", "SELECT 1 FROM DUAL", "Filtrar"
        ));

        assertEquals(AiProviderException.Category.PROVIDER_FAILURE, exception.getCategory());
        assertSame(providerFailure, exception.getCause());
        assertFalse(exception.getMessage().contains("provider detail"));
    }

    @Test
    @DisplayName("Debe clasificar como timeout la excepción de red del proveedor")
    void generateSql_ShouldClassifyProviderTimeout() {
        ChatModel chatModel = mock(ChatModel.class);
        when(chatModel.call(any(Prompt.class))).thenThrow(
                new RuntimeException(new SocketTimeoutException("provider timeout detail"))
        );
        AiSqlGeneratorService generator = new AiSqlGeneratorService(chatModel);

        AiProviderException exception = assertThrows(AiProviderException.class, () -> generator.generateSql(
                "Ventas por Cliente", "Sistema", "SELECT 1 FROM DUAL", "Filtrar"
        ));

        assertEquals(AiProviderException.Category.TIMEOUT, exception.getCategory());
    }

    @Test
    @DisplayName("Debe rechazar respuestas vacías del proveedor")
    void generateSql_ShouldRejectEmptyProviderResponse() {
        ChatModel chatModel = mock(ChatModel.class);
        ChatResponse response = mock(ChatResponse.class);
        Generation generation = mock(Generation.class);
        AssistantMessage output = mock(AssistantMessage.class);
        when(chatModel.call(any(Prompt.class))).thenReturn(response);
        when(response.getResult()).thenReturn(generation);
        when(generation.getOutput()).thenReturn(output);
        when(output.getText()).thenReturn("  ");
        AiSqlGeneratorService generator = new AiSqlGeneratorService(chatModel);

        AiProviderException exception = assertThrows(AiProviderException.class, () -> generator.generateSql(
                "Ventas por Cliente", "Sistema", "SELECT 1 FROM DUAL", "Filtrar"
        ));

        assertEquals(AiProviderException.Category.INVALID_RESPONSE, exception.getCategory());
    }
}
