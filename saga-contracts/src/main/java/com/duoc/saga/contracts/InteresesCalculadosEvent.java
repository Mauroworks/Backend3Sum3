package com.duoc.saga.contracts;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Evento emitido por account-service tras aplicar las tasas de interes mensual
 * a las cuentas migradas. Segundo paso de la Saga coreografica.
 *
 * @param evento      nombre del evento
 * @param sagaId      identificador de la instancia de la Saga
 * @param cuentas     detalle de cuentas con saldo final
 * @param totalInteres suma de intereses calculados
 * @param fecha       timestamp de emision
 */
public record InteresesCalculadosEvent(
        String evento,
        String sagaId,
        List<CuentaConInteres> cuentas,
        BigDecimal totalInteres,
        Instant fecha) {

    /**
     * Cuenta con su saldo final tras aplicar el interes.
     *
     * @param cuentaId  identificador de la cuenta
     * @param tipo      tipo de cuenta (ahorro, prestamo, hipoteca)
     * @param saldoBase saldo anterior a la aplicacion del interes
     * @param tasa      tasa aplicada
     * @param saldoFinal saldo resultante
     */
    public record CuentaConInteres(Long cuentaId, String tipo, BigDecimal saldoBase,
                                   BigDecimal tasa, BigDecimal saldoFinal) {
    }

    public static InteresesCalculadosEvent of(String sagaId, List<CuentaConInteres> cuentas, BigDecimal total) {
        return new InteresesCalculadosEvent(SagaEvents.INTERESES_CALCULADOS, sagaId, cuentas, total, Instant.now());
    }
}