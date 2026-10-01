package com.empresa.reportes.dto;

public record EjecutarReporteRequest(
        Long idReporte,
        String promptUsuario
) {
}
