package com.empresa.reportes.repository;

import com.empresa.reportes.domain.CatalogoReporte;
import com.empresa.reportes.dto.ReporteSummaryDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReporteRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private ReporteRepository reporteRepository;

    @Test
    @DisplayName("Debe consultar CATALOGO_REPORTES activos vía JDBC ordenados por categoría")
    void findAllActivos_ShouldQueryCatalogViaJdbc() {
        List<ReporteSummaryDto> expected = List.of(
                new ReporteSummaryDto(1L, "Ventas", "VENTAS", "Desc")
        );
        when(jdbcTemplate.query(eq(ReporteRepository.SQL_LISTAR_ACTIVOS), any(RowMapper.class)))
                .thenReturn(expected);

        List<ReporteSummaryDto> result = reporteRepository.findAllActivos();

        assertEquals(expected, result);
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), any(RowMapper.class));
        assertTrue(sqlCaptor.getValue().contains("CATALOGO_REPORTES"));
        assertTrue(sqlCaptor.getValue().contains("ESTADO = 'ACTIVO'"));
    }

    @Test
    @DisplayName("Debe buscar un reporte activo por id vía JDBC")
    void findActivoById_ShouldQueryById() {
        CatalogoReporte reporte = new CatalogoReporte(
                2L, "Finanzas", "FINANZAS", "Desc", "prompt", "SELECT 1 FROM DUAL"
        );
        when(jdbcTemplate.query(eq(ReporteRepository.SQL_FIND_BY_ID), any(RowMapper.class), eq(2L)))
                .thenReturn(List.of(reporte));

        Optional<CatalogoReporte> result = reporteRepository.findActivoById(2L);

        assertTrue(result.isPresent());
        assertEquals("Finanzas", result.get().nombre());
        verify(jdbcTemplate).query(eq(ReporteRepository.SQL_FIND_BY_ID), any(RowMapper.class), eq(2L));
    }

    @Test
    @DisplayName("Debe retornar vacío si el catálogo no contiene el id")
    void findActivoById_ShouldReturnEmpty() {
        when(jdbcTemplate.query(eq(ReporteRepository.SQL_FIND_BY_ID), any(RowMapper.class), anyLong()))
                .thenReturn(List.of());

        assertTrue(reporteRepository.findActivoById(99L).isEmpty());
    }
}
