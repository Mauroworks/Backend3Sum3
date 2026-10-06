package com.duoc.saga.contracts;

import java.time.Instant;
import java.util.List;

/**
 * Evento emitido por csv-result-api al finalizar la migracion de los CSV de
 * csvsemana3/. Da por iniciada la Saga coreografica.
 *
 * @param evento        nombre del evento
 * @param sagaId        identificador de la instancia de la Saga
 * @param filasMigradas cantidad de filas migradas desde los CSV
 * @param rechazadas    cantidad de filas descartadas por validacion
 * @param detalle       resumen por archivo CSV de origen
 * @param fecha         timestamp de emision
 */
public record MigracionDatosCompletadaEvent(
        String evento,
        String sagaId,
        int filasMigradas,
        int rechazadas,
        List<DetalleArchivo> detalle,
        Instant fecha) {

    /**
     * Detalle por CSV de origen.
     *
     * @param archivo  nombre del CSV
     * @param filas    filas leidas
     * @param validas  filas validadas correctamente
     * @param invalidas filas descartadas
     */
    public record DetalleArchivo(String archivo, int filas, int validas, int invalidas) {
    }

    public static MigracionDatosCompletadaEvent of(String sagaId, int filasMigradas, int rechazadas,
                                                   List<DetalleArchivo> detalle) {
        return new MigracionDatosCompletadaEvent(
                SagaEvents.MIGRACION_DATOS_COMPLETADA, sagaId, filasMigradas, rechazadas, detalle, Instant.now());
    }
}