package com.empresa.reportes.service;

import com.empresa.reportes.exception.SqlSecurityViolationException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import org.springframework.stereotype.Service;

@Service
public class SqlSecurityGuardrailService {

    /**
     * Valida y sanitiza la sentencia SQL generada por la IA.
     * 
     * @param rawSql Sentencia SQL bruta recibida
     * @return Sentencia SQL validada y con salvaguarda FETCH FIRST 500 ROWS ONLY
     * @throws SqlSecurityViolationException si viola alguna regla de seguridad
     */
    public String validateAndSanitize(String rawSql) {
        if (rawSql == null || rawSql.trim().isEmpty()) {
            throw new SqlSecurityViolationException("La sentencia SQL no puede estar vacía");
        }

        String cleanedSql = sanitizeMarkdown(rawSql);

        // 1. Rechazar comentarios SQL explícitos (-- o /* ... */)
        if (cleanedSql.contains("--") || cleanedSql.contains("/*")) {
            throw new SqlSecurityViolationException("Instrucción SQL denegada: Los comentarios SQL no están permitidos");
        }

        try {
            // 2. Parsear la sentencia mediante JSqlParser AST
            Statement statement = CCJSqlParserUtil.parse(cleanedSql);

            // 3. Verificar que sea exclusivamente SELECT
            if (!(statement instanceof Select select)) {
                throw new SqlSecurityViolationException("Instrucción SQL denegada: Solo se permiten consultas de tipo SELECT o WITH");
            }

            // 4. Inyectar salvaguarda FETCH FIRST 500 ROWS ONLY si no posee Límite/Fetch
            if (select instanceof PlainSelect plainSelect) {
                applyFetchGuardrail(plainSelect);
            } else if (select.getPlainSelect() != null) {
                applyFetchGuardrail(select.getPlainSelect());
            }

            return select.toString();
        } catch (SqlSecurityViolationException e) {
            throw e;
        } catch (Exception e) {
            throw new SqlSecurityViolationException("Error de sintaxis o seguridad SQL: " + e.getMessage());
        }
    }

    private String sanitizeMarkdown(String sql) {
        String trimmed = sql.trim();
        if (trimmed.startsWith("```sql")) {
            trimmed = trimmed.substring(6);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.trim();
    }

    private void applyFetchGuardrail(PlainSelect plainSelect) {
        if (plainSelect.getFetch() == null && plainSelect.getLimit() == null) {
            try {
                PlainSelect dummy = (PlainSelect) CCJSqlParserUtil.parse("SELECT 1 FROM DUAL FETCH FIRST 500 ROWS ONLY");
                plainSelect.setFetch(dummy.getFetch());
            } catch (Exception ignored) {
            }
        }
    }
}
