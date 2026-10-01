package com.empresa.reportes.service;

import com.empresa.reportes.domain.CatalogoReporte;
import com.empresa.reportes.dto.ColumnaMetadataDto;
import com.empresa.reportes.dto.EjecutarReporteRequest;
import com.empresa.reportes.dto.EjecutarReporteResponse;
import com.empresa.reportes.exception.AiProviderException;
import com.empresa.reportes.exception.ReporteNotFoundException;
import com.empresa.reportes.exception.SqlSecurityViolationException;
import com.empresa.reportes.repository.ReporteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DynamicReportServiceTest {

    @Mock
    private ReporteRepository reporteRepository;

    @Mock
    private AiSqlGeneratorService aiSqlGeneratorService;

    @Mock
    private JdbcTemplate jdbcTemplate;

    private DynamicReportService dynamicReportService;

    @BeforeEach
    void setUp() {
        dynamicReportService = new DynamicReportService(
                reporteRepository,
                aiSqlGeneratorService,
                new SqlSecurityGuardrailService(),
                jdbcTemplate
        );
    }

    @Test
    @DisplayName("Debe orquestar metadatos, Gemini, guardrail, JDBC y metadatos dinámicos del ResultSet")
    void ejecutar_ShouldRunFullPipeline() throws Exception {
        CatalogoReporte catalogo = catalogo();
        when(reporteRepository.findActivoById(1L)).thenReturn(Optional.of(catalogo));
        when(aiSqlGeneratorService.generateSql(
                catalogo.nombre(),
                catalogo.promptSistema(),
                catalogo.queryBase(),
                "solo región Norte"
        )).thenReturn("SELECT REGION, SUM(SUBTOTAL) AS TOTAL FROM V_VENTAS_DETALLADAS GROUP BY REGION");

        when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class))).thenAnswer(invocation -> {
            ResultSetExtractor<?> extractor = invocation.getArgument(1);
            ResultSet rs = org.mockito.Mockito.mock(ResultSet.class);
            ResultSetMetaData meta = org.mockito.Mockito.mock(ResultSetMetaData.class);
            when(rs.getMetaData()).thenReturn(meta);
            when(meta.getColumnCount()).thenReturn(2);
            when(meta.getColumnLabel(1)).thenReturn("REGION");
            when(meta.getColumnTypeName(1)).thenReturn("VARCHAR2");
            when(meta.getColumnLabel(2)).thenReturn("TOTAL");
            when(meta.getColumnTypeName(2)).thenReturn("NUMBER");
            when(rs.next()).thenReturn(true, false);
            when(rs.getObject(1)).thenReturn("Norte");
            when(rs.getObject(2)).thenReturn(1500);
            return extractor.extractData(rs);
        });

        EjecutarReporteResponse response = dynamicReportService.ejecutar(
                new EjecutarReporteRequest(1L, "solo región Norte")
        );

        assertEquals(1L, response.idReporte());
        assertEquals(catalogo.nombre(), response.nombreReporte());
        assertTrue(response.sqlEjecutado().toUpperCase().contains("FETCH"));
        assertEquals(List.of(
                new ColumnaMetadataDto("REGION", "VARCHAR2"),
                new ColumnaMetadataDto("TOTAL", "NUMBER")
        ), response.columnas());
        assertEquals("Norte", response.filas().getFirst().get("REGION"));
        assertEquals(1500, response.filas().getFirst().get("TOTAL"));

        var order = inOrder(reporteRepository, aiSqlGeneratorService, jdbcTemplate);
        order.verify(reporteRepository).findActivoById(1L);
        order.verify(aiSqlGeneratorService).generateSql(anyString(), anyString(), anyString(), anyString());
        order.verify(jdbcTemplate).query(anyString(), any(ResultSetExtractor.class));

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), any(ResultSetExtractor.class));
        assertFalse(sqlCaptor.getValue().contains("--"));
    }

    @Test
    @DisplayName("Debe rechazar SQL destructivo generado por la IA antes de tocar JDBC")
    void ejecutar_ShouldRejectMaliciousSql() {
        when(reporteRepository.findActivoById(1L)).thenReturn(Optional.of(catalogo()));
        when(aiSqlGeneratorService.generateSql(anyString(), anyString(), anyString(), anyString()))
                .thenReturn("DROP TABLE VENTAS");

        assertThrows(SqlSecurityViolationException.class, () ->
                dynamicReportService.ejecutar(new EjecutarReporteRequest(1L, "borra todo"))
        );
        org.mockito.Mockito.verifyNoInteractions(jdbcTemplate);
    }

    @Test
    @DisplayName("Debe detenerse si falla la generación de IA antes de tocar JDBC")
    void ejecutar_ShouldStopWhenAiGenerationFails() {
        when(reporteRepository.findActivoById(1L)).thenReturn(Optional.of(catalogo()));
        AiProviderException providerFailure = new AiProviderException(
                AiProviderException.Category.PROVIDER_FAILURE,
                "No se pudo generar la consulta con el proveedor de IA"
        );
        when(aiSqlGeneratorService.generateSql(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(providerFailure);

        AiProviderException thrown = assertThrows(AiProviderException.class, () ->
                dynamicReportService.ejecutar(new EjecutarReporteRequest(1L, "solo región Norte"))
        );

        assertSame(providerFailure, thrown);
        org.mockito.Mockito.verifyNoInteractions(jdbcTemplate);
    }

    @Test
    @DisplayName("Debe fallar si el reporte no existe en el catálogo")
    void ejecutar_ShouldThrowWhenMissing() {
        when(reporteRepository.findActivoById(88L)).thenReturn(Optional.empty());

        assertThrows(ReporteNotFoundException.class, () ->
                dynamicReportService.ejecutar(new EjecutarReporteRequest(88L, "cualquier cosa"))
        );
    }

    private static CatalogoReporte catalogo() {
        return new CatalogoReporte(
                1L,
                "Ventas y Servicios por Cliente y Región",
                "VENTAS",
                "Análisis dinámico",
                "Usa SELECT sobre V_VENTAS_DETALLADAS",
                "SELECT REGION FROM V_VENTAS_DETALLADAS"
        );
    }
}
