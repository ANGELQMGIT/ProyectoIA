package com.empresa.reportes.domain;

public record CatalogoReporte(
        Long id,
        String nombre,
        String categoria,
        String descripcion,
        String promptSistema,
        String queryBase
) {
}
