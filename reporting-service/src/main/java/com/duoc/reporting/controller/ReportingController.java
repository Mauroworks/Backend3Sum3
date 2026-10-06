package com.duoc.reporting.controller;

import com.duoc.reporting.model.Reporte;
import com.duoc.reporting.service.ReporteService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportingController {

    private final ReporteService reporteService;

    public ReportingController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @GetMapping
    @CircuitBreaker(name = "cliente-account-service", fallbackMethod = "fallback")
    public ResponseEntity<List<Reporte>> listar(@RequestParam(required = false) String sagaId) {
        return ResponseEntity.ok(sagaId == null || sagaId.isBlank()
                ? reporteService.todos()
                : reporteService.porSaga(sagaId));
    }

    private ResponseEntity<List<Reporte>> fallback(Exception e) {
        return ResponseEntity.ok(List.of());
    }
}