package com.empresa.reportes.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.empresa.reportes.dto.ColumnaMetadataDto;
import com.empresa.reportes.dto.EjecutarReporteResponse;
import com.empresa.reportes.dto.ReporteSummaryDto;
import com.empresa.reportes.exception.AiProviderException;
import com.empresa.reportes.exception.GlobalExceptionHandler;
import com.empresa.reportes.exception.ReporteNotFoundException;
import com.empresa.reportes.exception.SqlSecurityViolationException;
import com.empresa.reportes.filter.CorrelationIdFilter;
import com.empresa.reportes.service.DynamicReportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

@WebMvcTest(controllers = ReporteController.class)
@Import({GlobalExceptionHandler.class, CorrelationIdFilter.class})
class ReporteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DynamicReportService dynamicReportService;

    @Test
    @DisplayName("GET /api/v1/reportes retorna el catálogo activo")
    void listar_ShouldReturnCatalogJson() throws Exception {
        when(dynamicReportService.listarActivos()).thenReturn(List.of(
                new ReporteSummaryDto(1L, "Ventas", "VENTAS", "Descripción")
        ));

        mockMvc.perform(get("/api/v1/reportes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Ventas"))
                .andExpect(jsonPath("$[0].categoria").value("VENTAS"))
                .andExpect(jsonPath("$[0].descripcion").value("Descripción"));
    }

    @Test
    @DisplayName("POST /api/v1/reportes/ejecutar retorna grilla dinámica y SQL ejecutado")
    void ejecutar_ShouldReturnGridContract() throws Exception {
        when(dynamicReportService.ejecutar(any())).thenReturn(new EjecutarReporteResponse(
                1L,
                "Ventas",
                "SELECT REGION FROM V_VENTAS_DETALLADAS FETCH FIRST 500 ROWS ONLY",
                List.of(new ColumnaMetadataDto("REGION", "VARCHAR2")),
                List.of(Map.of("REGION", "Norte")),
                12L
        ));

        mockMvc.perform(post("/api/v1/reportes/ejecutar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idReporte":1,"promptUsuario":"solo Norte"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idReporte").value(1))
                .andExpect(jsonPath("$.nombreReporte").value("Ventas"))
                .andExpect(jsonPath("$.sqlEjecutado").exists())
                .andExpect(jsonPath("$.columnas[0].nombre").value("REGION"))
                .andExpect(jsonPath("$.filas[0].REGION").value("Norte"))
                .andExpect(jsonPath("$.tiempoEjecucionMs").value(12));
    }

    @Test
    @DisplayName("POST con SQL malicioso retorna HTTP 400 por guardrail")
    void ejecutar_ShouldReturn400OnSqlSecurityViolation() throws Exception {
        when(dynamicReportService.ejecutar(any()))
                .thenThrow(new SqlSecurityViolationException("Instrucción SQL denegada: Solo se permiten consultas de tipo SELECT o WITH"));

        mockMvc.perform(post("/api/v1/reportes/ejecutar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idReporte":1,"promptUsuario":"DROP TABLE VENTAS"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("SQL_SECURITY_VIOLATION"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST con reporte inexistente retorna HTTP 404")
    void ejecutar_ShouldReturn404WhenMissing() throws Exception {
        when(dynamicReportService.ejecutar(any())).thenThrow(new ReporteNotFoundException(99L));

        mockMvc.perform(post("/api/v1/reportes/ejecutar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idReporte":99,"promptUsuario":"hola"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("REPORTE_NOT_FOUND"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("POST con fallo del proveedor retorna error seguro y correlacionado")
    void ejecutar_ShouldReturnCorrelatedProviderError() throws Exception {
        when(dynamicReportService.ejecutar(any())).thenThrow(new AiProviderException(
                AiProviderException.Category.PROVIDER_FAILURE,
                "No se pudo generar la consulta con el proveedor de IA",
                new RuntimeException("raw provider response")
        ));

        MvcResult result = mockMvc.perform(post("/api/v1/reportes/ejecutar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idReporte":1,"promptUsuario":"reporte"}
                                """))
                .andExpect(status().isBadGateway())
                .andExpect(header().exists(CorrelationIdFilter.HEADER_NAME))
                .andExpect(jsonPath("$.error").value("AI_PROVIDER_ERROR"))
                .andExpect(jsonPath("$.mensaje").value("No se pudo generar la consulta con el proveedor de IA"))
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.traceId").exists())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        String responseTraceId = new ObjectMapper().readTree(responseBody).get("traceId").asText();
        assertEquals(result.getResponse().getHeader(CorrelationIdFilter.HEADER_NAME), responseTraceId);
        assertFalse(responseBody.contains("raw provider response"));
        assertFalse(responseBody.contains("RuntimeException"));
    }

    @Test
    @DisplayName("POST mapea indisponibilidad y timeout a estados HTTP distintos")
    void ejecutar_ShouldMapAiUnavailableAndTimeout() throws Exception {
        when(dynamicReportService.ejecutar(any()))
                .thenThrow(new AiProviderException(AiProviderException.Category.NOT_CONFIGURED, "safe"))
                .thenThrow(new AiProviderException(AiProviderException.Category.TIMEOUT, "safe"));

        mockMvc.perform(post("/api/v1/reportes/ejecutar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idReporte\":1,\"promptUsuario\":\"reporte\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("AI_UNAVAILABLE"));

        mockMvc.perform(post("/api/v1/reportes/ejecutar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idReporte\":1,\"promptUsuario\":\"reporte\"}"))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.error").value("AI_TIMEOUT"));
    }
}
