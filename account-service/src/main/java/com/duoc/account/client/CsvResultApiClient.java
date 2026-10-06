package com.duoc.account.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Cliente HTTP de csv-result-api, protegido con Resilience4j a nivel de metodo.
 * Propaga el token OAuth2 de servicio obtido del auth-server.
 * Si csv-result-api no esta disponible, devuelve una lista vacia para que la
 * Saga pueda continuar y se genere el evento de compensacion.
 */
@Service
public class CsvResultApiClient {

    private static final Logger log = LoggerFactory.getLogger(CsvResultApiClient.class);

    private final RestClient restClient;
    private final ServiceTokenProvider tokenProvider;
    private final com.fasterxml.jackson.databind.ObjectMapper mapper;

    @Value("${clients.csv-result-api.url:http://localhost:8085}")
    private String baseUrl;

    public CsvResultApiClient(RestClient.Builder builder, ServiceTokenProvider tokenProvider,
            com.fasterxml.jackson.databind.ObjectMapper mapper) {
        this.restClient = builder.build();
        this.tokenProvider = tokenProvider;
        this.mapper = mapper;
    }

    /**
     * @return lista de filas migradas; vacia si el servicio remoto no responde.
     */
    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(
            name = "cliente-csv-result-api", fallbackMethod = "fallback")
    @io.github.resilience4j.retry.annotation.Retry(name = "default")
    public List<FilaMigrada> obtenerResultadosMigrados(String sagaId) {
        String url = baseUrl + "/api/migracion/resultados?sagaId=" + sagaId;
        String token = tokenProvider.token();
        log.info("[CLIENTE] consultando csv-result-api: {} (con token de servicio: {})",
                url, token != null);

        try {

        // Se pide el body como String y se parsea con Jackson 2: en Spring Boot 4
        // el RestClient serializa con Jackson 3 (tools.jackson) y pedir un
        // JsonNode de Jackson 2 falla con "Type definition error".
        String json = restClient.get()
                .uri(url)
                .header("Authorization", "Bearer " + (token == null ? "" : token))
                .retrieve()
                .body(String.class);

        List<FilaMigrada> filas = new ArrayList<>();
        if (json == null || json.isBlank()) {
            return filas;
        }
        JsonNode node = mapper.readTree(json);
        if (node == null || !node.isArray()) {
            return filas;
        }
        for (JsonNode n : node) {
            filas.add(new FilaMigrada(
                    n.path("registro").asText(),
                    n.path("tipo").asText(),
                    n.path("monto").isMissingNode() ? BigDecimal.ZERO : n.path("monto").decimalValue(),
                    n.path("estado").asText()));
        }
        log.info("[CLIENTE] filas recibidas de csv-result-api: {}", filas.size());
        return filas;
        } catch (Exception e) {
            log.warn("[CLIENTE] no se pudo leer la respuesta de csv-result-api: {}", e.getMessage());
            throw new IllegalStateException("Fallo consultando csv-result-api: " + e.getMessage(), e);
        }
    }

    private List<FilaMigrada> fallback(Exception e) {
        log.warn("[RESILIENCIA] circuit breaker ABIERTO para csv-result-api ({}). Se devuelve lista vacia.",
                e.getMessage());
        return List.of();
    }

    /**
     * Fila migrada normalizada.
     *
     * @param registro id del registro en el CSV
     * @param tipo     tipo de movimiento
     * @param monto    monto migrado
     * @param estado   estado de la migracion
     */
    public record FilaMigrada(String registro, String tipo, BigDecimal monto, String estado) {
    }
}