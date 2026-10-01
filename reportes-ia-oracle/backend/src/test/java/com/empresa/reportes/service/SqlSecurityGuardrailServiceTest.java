package com.empresa.reportes.service;

import com.empresa.reportes.exception.SqlSecurityViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SqlSecurityGuardrailServiceTest {

    private SqlSecurityGuardrailService guardrailService;

    @BeforeEach
    void setUp() {
        guardrailService = new SqlSecurityGuardrailService();
    }

    @Test
    @DisplayName("Debe aprobar una consulta SELECT válida e inyectar FETCH FIRST 500 ROWS ONLY")
    void validSelect_ShouldPassAndInjectFetchFirst() {
        String input = "SELECT * FROM V_VENTAS_DETALLADAS WHERE ANIO_MES = '2026-09'";
        String result = guardrailService.validateAndSanitize(input);
        
        System.out.println("SQL SANITIZADO: " + result);
        assertNotNull(result);
        assertTrue(result.toUpperCase().contains("FETCH") || result.toUpperCase().contains("500"),
                "El SQL sanitizado debe contener la salvaguarda de límite de filas: " + result);
    }

    @Test
    @DisplayName("Debe rechazar sentencias de inserción DML")
    void insertStatement_ShouldThrowSecurityException() {
        String input = "INSERT INTO CLIENTES (NOMBRE) VALUES ('Hacker')";
        
        assertThrows(SqlSecurityViolationException.class, () -> {
            guardrailService.validateAndSanitize(input);
        });
    }

    @Test
    @DisplayName("Debe rechazar sentencias destructivas DDL DROP")
    void dropStatement_ShouldThrowSecurityException() {
        String input = "DROP TABLE VENTAS";
        
        assertThrows(SqlSecurityViolationException.class, () -> {
            guardrailService.validateAndSanitize(input);
        });
    }

    @Test
    @DisplayName("Debe rechazar consultas que contengan comentarios SQL")
    void sqlWithComment_ShouldThrowSecurityException() {
        String input = "SELECT * FROM CATALOGO_REPORTES -- comentario malicioso";
        
        assertThrows(SqlSecurityViolationException.class, () -> {
            guardrailService.validateAndSanitize(input);
        });
    }
}
