package com.empresa.reportes.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.empresa.reportes.exception.AiProviderException;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;

@Service
public class AiSqlGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(AiSqlGeneratorService.class);

    private final ChatModel chatModel;

    @Autowired
    public AiSqlGeneratorService(ObjectProvider<ChatModel> chatModelProvider) {
        this(chatModelProvider.getIfAvailable());
    }

    AiSqlGeneratorService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * Genera una consulta SQL de solo lectura utilizando Gemini AI.
     *
     * @param nombreReporte  Nombre del reporte del catálogo
     * @param promptSistema  Reglas de negocio e instrucciones del sistema
     * @param queryBase      Query SQL base del reporte
     * @param promptUsuario  Instrucción en lenguaje natural ingresada por el usuario
     * @return Sentencia SQL generada
     */
    public String generateSql(String nombreReporte, String promptSistema, String queryBase, String promptUsuario) {
        String fullPrompt = String.format("""
            %s
            
            [CONTEXTO DEL REPORTE]
            Nombre Reporte: %s
            Query Base: %s
            
            [PETICIÓN DEL USUARIO]
            %s
            
            [INSTRUCCIONES E STRICTAS DE SALIDA]
            Responde ÚNICAMENTE con la consulta SQL ejecutable en Oracle DB. 
            NO agregues explicaciones, NO agregues bloques Markdown (```sql), NO agregues comentarios SQL.
            """, promptSistema, nombreReporte, queryBase, promptUsuario);

        log.debug("Enviando Prompt a Gemini AI para reporte '{}'...", nombreReporte);

        if (chatModel == null) {
            throw new AiProviderException(
                    AiProviderException.Category.NOT_CONFIGURED,
                    "El proveedor de IA no está disponible"
            );
        }

        try {
            var response = chatModel.call(new Prompt(fullPrompt));
            if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
                throw new AiProviderException(
                        AiProviderException.Category.INVALID_RESPONSE,
                        "El proveedor de IA devolvió una respuesta vacía"
                );
            }

            String sql = response.getResult().getOutput().getText();
            if (sql == null || sql.isBlank()) {
                throw new AiProviderException(
                        AiProviderException.Category.INVALID_RESPONSE,
                        "El proveedor de IA devolvió una respuesta vacía"
                );
            }

            log.debug("Respuesta SQL recibida del proveedor para el reporte '{}'", nombreReporte);
            return sql;
        } catch (AiProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new AiProviderException(
                    isTimeout(e) ? AiProviderException.Category.TIMEOUT : AiProviderException.Category.PROVIDER_FAILURE,
                    "No se pudo generar la consulta con el proveedor de IA",
                    e
            );
        }
    }

    private boolean isTimeout(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof TimeoutException || current instanceof SocketTimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
