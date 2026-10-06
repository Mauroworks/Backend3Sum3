package com.duoc.account.event;

import com.duoc.saga.contracts.InteresesCalculadosEvent;
import com.duoc.saga.contracts.MigracionDatosCompletadaEvent;
import com.duoc.saga.contracts.SagaCompensadaEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Configuration
public class SagaFunctionConfig {

    private static final Logger log = LoggerFactory.getLogger(SagaFunctionConfig.class);

/**
 * Reaccion choreografica al evento MIGRACION_DATOS_COMPLETADA.
 * <p>
 * El consumidor recibe como {@code Object} y decide por el campo "evento" del
 * JSON: account-service publica en el mismo topic que consume, asi que tambien
 * le llegan sus propios eventos (INTERESES_CALCULADOS, REPORTE_GENERADO) y un
 * Consumer tipado fallaria al deserializarlos.
 */
@Bean
public java.util.function.Consumer<Object> sagaEvents(
        com.duoc.account.client.CsvResultApiClient client,
        com.duoc.account.service.CuentaService cuentaService,
        SagaEventPublisher publisher,
        ObjectMapper mapper) {

    return entrada -> {
        String json = aJson(entrada, mapper);
        String tipoEvento = leerCampo(entrada, mapper, "evento");

        switch (tipoEvento == null ? "" : tipoEvento) {
            case "MIGRACION_DATOS_COMPLETADA" -> procesarMigracion(json, client, cuentaService, publisher, mapper);
            case "SAGA_COMPENSADA" -> {
                SagaCompensadaEvent e = convertir(json, mapper, SagaCompensadaEvent.class);
                log.warn("[SAGA-RECIBIDO] {} sagaId={} pasoFallido={} motivo={}",
                        e.evento(), e.sagaId(), e.pasoFallido(), e.motivo());
            }
            default -> log.info("[SAGA] evento ignorado por account-service: {}", tipoEvento);
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

private void procesarMigracion(String json,
        com.duoc.account.client.CsvResultApiClient client,
        com.duoc.account.service.CuentaService cuentaService,
        SagaEventPublisher publisher,
        ObjectMapper mapper) {

    MigracionDatosCompletadaEvent evento = convertir(json, mapper, MigracionDatosCompletadaEvent.class);
    log.info("[SAGA-RECIBIDO] {} sagaId={} filasMigradas={} rechazadas={}",
            evento.evento(), evento.sagaId(), evento.filasMigradas(), evento.rechazadas());

    List<com.duoc.account.client.CsvResultApiClient.FilaMigrada> filas =
            client.obtenerResultadosMigrados(evento.sagaId());

    if (filas.isEmpty()) {
        log.warn("[SAGA] sagaId={} sin filas migradas disponibles, se emite compensacion", evento.sagaId());
        publisher.publicar(SagaCompensadaEvent.of(evento.sagaId(), "CONSULTA_MIGRACION",
                "csv-result-api no devolvio filas migradas"));
        return;
    }

    List<InteresesCalculadosEvent.CuentaConInteres> cuentas = filas.stream()
            .filter(f -> "MIGRADO".equals(f.estado()) || "MIGRADO_CON_ANOMALIA".equals(f.estado()))
            .map(f -> new InteresesCalculadosEvent.CuentaConInteres(
                    cuentaIdDe(f.registro()), f.tipo(), f.monto(), BigDecimal.ZERO, f.monto()))
            .toList();

    InteresesCalculadosEvent resultado = cuentaService.calcular(evento.sagaId(), cuentas);
    publisher.publicar(resultado);
}

private String leerCampo(Object entrada, ObjectMapper mapper, String campo) {
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

    private Long cuentaIdDe(String registro) {
        try {
            return Long.parseLong(registro.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return Math.abs((long) registro.hashCode());
        }
    }
}