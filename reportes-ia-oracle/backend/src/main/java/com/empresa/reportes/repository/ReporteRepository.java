package com.empresa.reportes.repository;

import com.empresa.reportes.domain.CatalogoReporte;
import com.empresa.reportes.dto.ReporteSummaryDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class ReporteRepository {

    static final String SQL_LISTAR_ACTIVOS = """
            SELECT ID_REPORTE, NOMBRE_REPORTE, CATEGORIA, DESCRIPCION
            FROM CATALOGO_REPORTES
            WHERE ESTADO = 'ACTIVO'
            ORDER BY CATEGORIA, NOMBRE_REPORTE
            """;

    static final String SQL_FIND_BY_ID = """
            SELECT ID_REPORTE, NOMBRE_REPORTE, CATEGORIA, DESCRIPCION, PROMPT_SISTEMA, QUERY_BASE
            FROM CATALOGO_REPORTES
            WHERE ID_REPORTE = ? AND ESTADO = 'ACTIVO'
            """;

    private final JdbcTemplate jdbcTemplate;

    public ReporteRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ReporteSummaryDto> findAllActivos() {
        return jdbcTemplate.query(SQL_LISTAR_ACTIVOS, summaryMapper());
    }

    public Optional<CatalogoReporte> findActivoById(Long idReporte) {
        List<CatalogoReporte> rows = jdbcTemplate.query(SQL_FIND_BY_ID, catalogoMapper(), idReporte);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(rows.getFirst());
    }

    private RowMapper<ReporteSummaryDto> summaryMapper() {
        return (rs, rowNum) -> new ReporteSummaryDto(
                rs.getLong("ID_REPORTE"),
                rs.getString("NOMBRE_REPORTE"),
                rs.getString("CATEGORIA"),
                rs.getString("DESCRIPCION")
        );
    }

    private RowMapper<CatalogoReporte> catalogoMapper() {
        return (rs, rowNum) -> new CatalogoReporte(
                rs.getLong("ID_REPORTE"),
                rs.getString("NOMBRE_REPORTE"),
                rs.getString("CATEGORIA"),
                rs.getString("DESCRIPCION"),
                readClobOrString(rs, "PROMPT_SISTEMA"),
                readClobOrString(rs, "QUERY_BASE")
        );
    }

    private static String readClobOrString(ResultSet rs, String column) throws SQLException {
        String value = rs.getString(column);
        return value == null ? "" : value;
    }
}
