package com.duoc.account.controller;

import com.duoc.account.model.Cuenta;
import com.duoc.account.service.CuentaService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final CuentaService cuentaService;

    public AccountController(CuentaService cuentaService) {
        this.cuentaService = cuentaService;
    }

    @GetMapping
    @CircuitBreaker(name = "cliente-csv-result-api", fallbackMethod = "fallback")
    public ResponseEntity<List<Cuenta>> listar(@RequestParam(required = false) String sagaId) {
        return ResponseEntity.ok(sagaId == null || sagaId.isBlank()
                ? cuentaService.todas()
                : cuentaService.porSaga(sagaId));
    }

    @GetMapping("/resumen")
    @CircuitBreaker(name = "cliente-csv-result-api", fallbackMethod = "fallbackResumen")
    public ResponseEntity<Map<String, Object>> resumen() {
        List<Cuenta> cuentas = cuentaService.todas();
        return ResponseEntity.ok(Map.of(
                "totalCuentas", cuentas.size(),
                "sagasProcesadas", cuentas.stream().map(Cuenta::getSagaId).distinct().count()));
    }

    private ResponseEntity<List<Cuenta>> fallback(Exception e) {
        return ResponseEntity.ok(List.of());
    }

    private ResponseEntity<Map<String, Object>> fallbackResumen(Exception e) {
        return ResponseEntity.ok(Map.of("estado", "DEGRADADO"));
    }
}