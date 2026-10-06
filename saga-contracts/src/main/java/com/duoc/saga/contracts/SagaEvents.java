package com.duoc.saga.contracts;

/**
 * Nombres de los eventos de la Saga coreografica del Banco XYZ.
 * Cada microservicio publica y/o consume estos eventos a traves de Kafka
 * (Spring Cloud Stream) usando el topic unico saga.banco.xyz.
 */
public final class SagaEvents {

    /** Evento 1: csv-result-api finishes the CSV migration and publishes the result. */
    public static final String MIGRACION_DATOS_COMPLETADA = "MIGRACION_DATOS_COMPLETADA";

    /** Evento 2: account-service reacts by calculating monthly interest. */
    public static final String INTERESES_CALCULADOS = "INTERESES_CALCULADOS";

    /** Evento 3: reporting-service reacts by consolidating the annual statement. */
    public static final String REPORTE_GENERADO = "REPORTE_GENERADO";

    /** Evento 4: compensacion emitida cuando un paso de la Saga falla. */
    public static final String SAGA_COMPENSADA = "SAGA_COMPENSADA";

    private SagaEvents() {
    }
}