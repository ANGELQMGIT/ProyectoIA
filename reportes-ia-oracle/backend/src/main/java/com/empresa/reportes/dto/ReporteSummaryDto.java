package com.empresa.reportes.dto;

public record ReporteSummaryDto(
        Long id,
        String nombre,
        String categoria,
        String descripcion
) {
}
