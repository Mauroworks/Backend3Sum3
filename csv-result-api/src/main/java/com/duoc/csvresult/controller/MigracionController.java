package com.duoc.csvresult.controller;

import com.duoc.csvresult.event.SagaEventPublisher;
import com.duoc.csvresult.model.ResultadoMigracion;
import com.duoc.csvresult.service.MigracionCsvService;
import com.duoc.csvresult.service.ResultadoMigracionService;
import com.duoc.saga.contracts.MigracionDatosCompletadaEvent;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/migracion")
public class MigracionController {

    private static final Logger log = LoggerFactory.getLogger(MigracionController.class);

    private final MigracionCsvService migracionCsvService;
    private final ResultadoMigracionService resultadoService;
    private final SagaEventPublisher eventPublisher;

    @Value("${batch.csv.path:csvsemana3}")
    private String csvPath;

    public MigracionController(MigracionCsvService migracionCsvService,
                               ResultadoMigracionService resultadoService,
                               SagaEventPublisher eventPublisher) {
        this.migracionCsvService = migracionCsvService;
        this.resultadoService = resultadoService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Ejecuta la migracion de los CSV y publica MIGRACION_DATOS_COMPLETADA,
     * evento que abre la Saga coreografica.
     */
    @PostMapping("/ejecutar")
    @CircuitBreaker(name = "consulta-resultados-migracion", fallbackMethod = "fallbackMigracion")
    @Retry(name = "default")
    public ResponseEntity<?> ejecutar(@RequestParam(required = false) String ruta) {
        MigracionDatosCompletadaEvent evento = migracionCsvService.migrar(
                ruta == null || ruta.isBlank() ? csvPath : ruta);
        eventPublisher.publicar(evento);
        return ResponseEntity.ok(evento);
    }

    @GetMapping("/resultados")
    @CircuitBreaker(name = "consulta-resultados-migracion", fallbackMethod = "fallbackResultados")
    public ResponseEntity<List<ResultadoMigracion>> resultados(
            @RequestParam(required = false) String sagaId,
            @RequestParam(required = false) String archivo) {
        if (sagaId != null && !sagaId.isBlank()) {
            return ResponseEntity.ok(resultadoService.porSaga(sagaId));
        }
        if (archivo != null && !archivo.isBlank()) {
            return ResponseEntity.ok(resultadoService.todos().stream()
                    .filter(r -> r.getArchivoOrigen().equals(archivo))
                    .toList());
        }
        return ResponseEntity.ok(resultadoService.todos());
    }

    @GetMapping("/resumen")
    @CircuitBreaker(name = "consulta-resultados-migracion", fallbackMethod = "fallbackResumen")
    public ResponseEntity<Map<String, Object>> resumen() {
        return ResponseEntity.ok(resultadoService.resumen());
    }

    @GetMapping("/health-resiliencia")
    public ResponseEntity<Map<String, Object>> estadoResiliencia() {
        return ResponseEntity.ok(Map.of(
                "circuitBreaker", "consulta-resultados-migracion",
                "retry", "default (3 intentos con backoff exponencial)",
                "fallbackMethod", "fallbackMigracion / fallbackResultados / fallbackResumen"));
    }

    private ResponseEntity<?> fallbackMigracion(Exception e) {
        log.warn("[RESILIENCIA] circuit breaker abierto, devolviendo respuesta degradada: {}", e.getMessage());
        return ResponseEntity.status(503).body(Map.of(
                "estado", "DEGRADADO",
                "motivo", "Migracion no disponible temporalmente",
                "detalle", String.valueOf(e.getMessage())));
    }

    private ResponseEntity<List<ResultadoMigracion>> fallbackResultados(Exception e) {
        log.warn("[RESILIENCIA] fallback de resultados activado: {}", e.getMessage());
        return ResponseEntity.ok(List.of());
    }

    private ResponseEntity<Map<String, Object>> fallbackResumen(Exception e) {
        log.warn("[RESILIENCIA] fallback de resumen activado: {}", e.getMessage());
        return ResponseEntity.ok(Map.of("estado", "DEGRADADO"));
    }
}