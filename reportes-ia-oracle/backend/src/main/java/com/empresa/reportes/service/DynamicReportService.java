package com.empresa.reportes.service;

import com.empresa.reportes.domain.CatalogoReporte;
import com.empresa.reportes.dto.ColumnaMetadataDto;
import com.empresa.reportes.dto.EjecutarReporteRequest;
import com.empresa.reportes.dto.EjecutarReporteResponse;
import com.empresa.reportes.dto.ReporteSummaryDto;
import com.empresa.reportes.exception.ReporteNotFoundException;
import com.empresa.reportes.repository.ReporteRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Service;

import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DynamicReportService {

    private final ReporteRepository reporteRepository;
    private final AiSqlGeneratorService aiSqlGeneratorService;
    private final SqlSecurityGuardrailService sqlSecurityGuardrailService;
    private final JdbcTemplate jdbcTemplate;

    public DynamicReportService(
            ReporteRepository reporteRepository,
            AiSqlGeneratorService aiSqlGeneratorService,
            SqlSecurityGuardrailService sqlSecurityGuardrailService,
            JdbcTemplate jdbcTemplate
    ) {
        this.reporteRepository = reporteRepository;
        this.aiSqlGeneratorService = aiSqlGeneratorService;
        this.sqlSecurityGuardrailService = sqlSecurityGuardrailService;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ReporteSummaryDto> listarActivos() {
        return reporteRepository.findAllActivos();
    }

    public EjecutarReporteResponse ejecutar(EjecutarReporteRequest request) {
        long started = System.nanoTime();

        CatalogoReporte reporte = reporteRepository.findActivoById(request.idReporte())
                .orElseThrow(() -> new ReporteNotFoundException(request.idReporte()));

        String generatedSql = aiSqlGeneratorService.generateSql(
                reporte.nombre(),
                reporte.promptSistema(),
                reporte.queryBase(),
                request.promptUsuario()
        );

        String sqlSeguro = sqlSecurityGuardrailService.validateAndSanitize(generatedSql);
        QueryResult resultado = jdbcTemplate.query(sqlSeguro, resultSetExtractor());

        long elapsedMs = (System.nanoTime() - started) / 1_000_000L;
        return new EjecutarReporteResponse(
                reporte.id(),
                reporte.nombre(),
                sqlSeguro,
                resultado.columnas(),
                resultado.filas(),
                elapsedMs
        );
    }

    private ResultSetExtractor<QueryResult> resultSetExtractor() {
        return rs -> {
            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();
            List<ColumnaMetadataDto> columnas = new ArrayList<>(columnCount);
            for (int i = 1; i <= columnCount; i++) {
                columnas.add(new ColumnaMetadataDto(meta.getColumnLabel(i), meta.getColumnTypeName(i)));
            }

            List<Map<String, Object>> filas = new ArrayList<>();
            while (rs.next()) {
                filas.add(mapRow(rs, columnas));
            }
            return new QueryResult(columnas, filas);
        };
    }

    private static Map<String, Object> mapRow(ResultSet rs, List<ColumnaMetadataDto> columnas) throws SQLException {
        Map<String, Object> fila = new LinkedHashMap<>();
        for (int i = 1; i <= columnas.size(); i++) {
            Object value = rs.getObject(i);
            if (value instanceof Clob clob) {
                value = clob.getSubString(1, (int) Math.min(clob.length(), Integer.MAX_VALUE));
            }
            fila.put(columnas.get(i - 1).nombre(), value);
        }
        return fila;
    }

    record QueryResult(List<ColumnaMetadataDto> columnas, List<Map<String, Object>> filas) {
    }
}
