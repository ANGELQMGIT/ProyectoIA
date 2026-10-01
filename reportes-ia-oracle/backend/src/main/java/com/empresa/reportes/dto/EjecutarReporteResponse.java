package com.empresa.reportes.dto;

import java.util.List;
import java.util.Map;

public record EjecutarReporteResponse(
        Long idReporte,
        String nombreReporte,
        String sqlEjecutado,
        List<ColumnaMetadataDto> columnas,
        List<Map<String, Object>> filas,
        long tiempoEjecucionMs
) {
}
