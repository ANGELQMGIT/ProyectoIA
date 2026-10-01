package com.empresa.reportes.controller;

import com.empresa.reportes.dto.EjecutarReporteRequest;
import com.empresa.reportes.dto.EjecutarReporteResponse;
import com.empresa.reportes.dto.ReporteSummaryDto;
import com.empresa.reportes.service.DynamicReportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reportes")
public class ReporteController {

    private final DynamicReportService dynamicReportService;

    public ReporteController(DynamicReportService dynamicReportService) {
        this.dynamicReportService = dynamicReportService;
    }

    @GetMapping
    public List<ReporteSummaryDto> listar() {
        return dynamicReportService.listarActivos();
    }

    @PostMapping("/ejecutar")
    public EjecutarReporteResponse ejecutar(@RequestBody EjecutarReporteRequest request) {
        return dynamicReportService.ejecutar(request);
    }
}
