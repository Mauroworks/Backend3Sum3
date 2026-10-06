package com.duoc.saga.contracts;

import java.time.Instant;

/**
 * Evento de compensacion de la Saga coreografica. Se emite cuando un paso
 * falla y se activa el flujo de rollback logico.
 *
 * @param evento      nombre del evento
 * @param sagaId      identificador de la instancia de la Saga
 * @param pasoFallido paso que fallo
 * @param motivo      causa del fallo
 * @param fecha       timestamp de emision
 */
public record SagaCompensadaEvent(String evento, String sagaId, String pasoFallido,
                                  String motivo, Instant fecha) {

    public static SagaCompensadaEvent of(String sagaId, String pasoFallido, String motivo) {
        return new SagaCompensadaEvent(SagaEvents.SAGA_COMPENSADA, sagaId, pasoFallido, motivo, Instant.now());
    }
}