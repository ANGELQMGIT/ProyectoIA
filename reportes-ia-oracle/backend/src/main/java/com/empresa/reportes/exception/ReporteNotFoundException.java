package com.empresa.reportes.exception;

public class ReporteNotFoundException extends RuntimeException {

    private final Long idReporte;

    public ReporteNotFoundException(Long idReporte) {
        super("No se encontró un reporte activo con id " + idReporte);
        this.idReporte = idReporte;
    }

    public Long getIdReporte() {
        return idReporte;
    }
}
