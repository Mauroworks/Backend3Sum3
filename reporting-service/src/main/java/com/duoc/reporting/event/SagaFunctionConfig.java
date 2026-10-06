package com.duoc.reporting.event;

import com.duoc.reporting.model.Reporte;
import com.duoc.reporting.service.ReporteService;
import com.duoc.saga.contracts.InteresesCalculadosEvent;
import com.duoc.saga.contracts.MigracionDatosCompletadaEvent;
import com.duoc.saga.contracts.ReporteGeneradoEvent;
import com.duoc.saga.contracts.SagaCompensadaEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

@Configuration
public class SagaFunctionConfig {

    private static final Logger log = LoggerFactory.getLogger(SagaFunctionConfig.class);

    /**
     * Tercer paso de la Saga coreografica: consolida y emite el reporte.
     * Spring Cloud Stream enruta por el campo "evento" del JSON.
     */
    @Bean
    public java.util.function.Consumer<Object> sagaEvents(ReporteService reporteService,
                                                          StreamBridge streamBridge,
                                                          ObjectMapper mapper) {
        return entrada -> {
            String json = aJson(entrada, mapper);
            String tipoEvento = leer(entrada, mapper, "evento");

            switch (tipoEvento == null ? "" : tipoEvento) {
                case "MIGRACION_DATOS_COMPLETADA" -> {
                    MigracionDatosCompletadaEvent e = convertir(json, mapper, MigracionDatosCompletadaEvent.class);
                    log.info("[SAGA-RECIBIDO] {} sagaId={} filasMigradas={} rechazadas={}",
                            e.evento(), e.sagaId(), e.filasMigradas(), e.rechazadas());
                    Reporte r = reporteService.guardar(new Reporte(e.sagaId(), "RESUMEN_MIGRACION",
                            "GENERADO", e.filasMigradas(), BigDecimal.ZERO,
                            "Filas migradas=" + e.filasMigradas() + ", rechazadas=" + e.rechazadas()));
                    publicar(streamBridge, mapper, ReporteGeneradoEvent.of(e.sagaId(), r.getId(),
                            "RESUMEN_MIGRACION", e.filasMigradas(), BigDecimal.ZERO));
                }
                case "INTERESES_CALCULADOS" -> {
                    InteresesCalculadosEvent e = convertir(json, mapper, InteresesCalculadosEvent.class);
                    log.info("[SAGA-RECIBIDO] {} sagaId={} cuentas={} totalInteres={}",
                            e.evento(), e.sagaId(), e.cuentas().size(), e.totalInteres());
                    Reporte r = reporteService.guardar(new Reporte(e.sagaId(), "ESTADO_CUENTA_ANUAL",
                            "GENERADO", e.cuentas().size(), e.totalInteres(),
                            "Total intereses=" + e.totalInteres()));
                    publicar(streamBridge, mapper, ReporteGeneradoEvent.of(e.sagaId(), r.getId(),
                            "ESTADO_CUENTA_ANUAL", e.cuentas().size(), e.totalInteres()));
                }
                case "SAGA_COMPENSADA" -> {
                    SagaCompensadaEvent e = convertir(json, mapper, SagaCompensadaEvent.class);
                    log.warn("[SAGA-RECIBIDO] {} sagaId={} pasoFallido={} motivo={}",
                            e.evento(), e.sagaId(), e.pasoFallido(), e.motivo());
                    reporteService.guardar(new Reporte(e.sagaId(), "COMPENSACION",
                            "COMPENSADO", 0, BigDecimal.ZERO,
                            "pasoFallido=" + e.pasoFallido() + ", motivo=" + e.motivo()));
                }
                case "REPORTE_GENERADO" -> log.info("[SAGA-CIERRE] {}", json);
                default -> log.warn("[SAGA] evento ignorado: {}", tipoEvento);
            }
        };
    }

    /**
     * Convierte el payload JSON a record del contrato. Se usa readValue sobre el
     * texto y no convertValue porque el binder entrega byte[] y convertValue no
     * sabe deserializarlo.
     */
    private <T> T convertir(String json, ObjectMapper mapper, Class<T> tipo) {
        try {
            return mapper.readValue(json, tipo);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo convertir el evento a " + tipo.getSimpleName(), e);
        }
    }

    private String leer(Object entrada, ObjectMapper mapper, String campo) {
        if (entrada == null) {
            return null;
        }
        // Con contentType application/json y un Consumer<Object> el binder
        // entrega el payload como byte[]; hay que decodificarlo antes de
        // leer el campo o el nombre del evento saldria vacio.
        String json = aJson(entrada, mapper);
        if (json == null) {
            return null;
        }
        try {
            return mapper.readTree(json).path(campo).asText();
        } catch (Exception e) {
            return null;
        }
    }

    private String aJson(Object entrada, ObjectMapper mapper) {
        if (entrada instanceof byte[] bytes) {
            return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        }
        if (entrada instanceof String s) {
            return s;
        }
        return mapper.valueToTree(entrada).toString();
    }

    private void publicar(StreamBridge streamBridge, ObjectMapper mapper, Object evento) {
        try {
            boolean ok = streamBridge.send("sagaEvents-out-0", mapper.writeValueAsString(evento));
            log.info(ok ? "[SAGA-PUB] REPORTE_GENERADO publicado" : "[SAGA-PUB] broker no disponible");
        } catch (Exception e) {
            log.error("[SAGA-PUB] error publicando: {}", e.getMessage());
        }
    }
}