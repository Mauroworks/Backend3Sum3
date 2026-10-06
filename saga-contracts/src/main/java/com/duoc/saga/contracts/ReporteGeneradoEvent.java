package com.duoc.saga.contracts;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Evento emitido por reporting-service al consolidar el estado de cuenta anual.
 * Ultimo paso de la Saga coreografica.
 *
 * @param evento        nombre del evento
 * @param sagaId        identificador de la instancia de la Saga
 * @param reporte       identificador del reporte generado
 * @param tipoReporte   tipo de reporte (ESTADO_CUENTA_ANUAL, RESUMEN_DIARIO)
 * @param totalCuentas  cantidad de cuentas consolidadas
 * @param saldoTotal    saldo total consolidado
 * @param fecha         timestamp de emision
 */
public record ReporteGeneradoEvent(
        String evento,
        String sagaId,
        Long reporte,
        String tipoReporte,
        int totalCuentas,
        BigDecimal saldoTotal,
        Instant fecha) {

    public static ReporteGeneradoEvent of(String sagaId, Long reporte, String tipoReporte,
                                          int totalCuentas, BigDecimal saldoTotal) {
        return new ReporteGeneradoEvent(SagaEvents.REPORTE_GENERADO, sagaId, reporte, tipoReporte,
                totalCuentas, saldoTotal, Instant.now());
    }
}