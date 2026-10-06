package com.duoc.csvresult.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

@Component
public class SagaEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(SagaEventPublisher.class);

    private final StreamBridge streamBridge;
    private final ObjectMapper objectMapper;

    public SagaEventPublisher(StreamBridge streamBridge, ObjectMapper objectMapper) {
        this.streamBridge = streamBridge;
        this.objectMapper = objectMapper;
    }

    /**
     * Publica un evento de la Saga en el binding de salida.
     * En modo local, si Kafka no esta disponible, la falla se registra pero
     * no detiene la migracion (arquitectura tolerante a fallos).
     */
    public void publicar(Object evento) {
        try {
            String payload = objectMapper.writeValueAsString(evento);
            boolean ok = streamBridge.send("sagaEvents-out-0", payload);
            if (ok) {
                log.info("[SAGA-PUB] evento publicado en Kafka topic saga.banco.xyz -> {}",
                        objectMapper.readTree(payload).path("evento").asText());
            } else {
                log.warn("[SAGA-PUB] broker no disponible, evento NO publicado: {}",
                        objectMapper.readTree(payload).path("evento").asText());
            }
        } catch (JsonProcessingException e) {
            log.error("[SAGA-PUB] no se pudo serializar el evento: {}", e.getMessage());
        }
    }
}