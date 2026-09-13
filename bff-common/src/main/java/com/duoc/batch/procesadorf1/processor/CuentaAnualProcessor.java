package com.duoc.batch.procesadorf1.processor;

import com.duoc.batch.procesadorf1.exception.DataValidationException;
import com.duoc.batch.procesadorf1.model.MovimientoAnual;
import com.duoc.batch.procesadorf1.model.RawCuentaAnual;
import com.duoc.batch.procesadorf1.util.DateParser;
import org.springframework.batch.infrastructure.item.ItemProcessor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Valida y transforma los movimientos anuales.
 *
 * Reglas:
 *  - cuenta_id: valor numerico obligatorio.
 *  - fecha: fecha valida (varios formatos), si no se rechaza.
 *  - transaccion: validos deposito/retiro/compra/pago.
 *    Se normaliza 'deposito' (con acento) a 'deposito'.
 *  - monto: numerico obligatorio; vacio se corrige a 0.
 *  - Montos negativos se conservan y se marcan como anomalia.
 */
public class CuentaAnualProcessor implements ItemProcessor<RawCuentaAnual, MovimientoAnual> {

    private static final Set<String> TIPOS = new HashSet<>(
            Arrays.asList("deposito", "retiro", "compra", "pago"));

    @Override
    public MovimientoAnual process(RawCuentaAnual item) {
        if (item == null) {
            return null;
        }

        // --- cuenta_id ---
        Long cuentaId;
        try {
            cuentaId = Long.valueOf(item.getCuentaId().trim());
        } catch (Exception e) {
            throw new DataValidationException("cuenta_id no valido: " + item.getCuentaId());
        }

        String cuenta = String.valueOf(cuentaId);

        // --- fecha ---
        LocalDate fecha = DateParser.parse(item.getFecha());
        if (fecha == null) {
            throw new DataValidationException("Movimiento cuenta " + cuenta
                    + " con fecha no valida: " + item.getFecha());
        }

        // --- transaccion (normalizar acento) ---
        String transaccion = item.getTransaccion() == null ? "" : item.getTransaccion().trim().toLowerCase();
        if ("depósito".equals(transaccion)) {
            transaccion = "deposito";
        }
        if (!TIPOS.contains(transaccion)) {
            throw new DataValidationException("Movimiento cuenta " + cuenta
                    + " con tipo de transaccion no valido: " + item.getTransaccion());
        }

        // --- monto (correccion si vacio) ---
        BigDecimal monto;
        String estado = "OK";
        try {
            if (item.getMonto() == null || item.getMonto().isBlank()) {
                monto = BigDecimal.ZERO;
                estado = "MONTO_CORREGIDO";
            } else {
                monto = new BigDecimal(item.getMonto().trim());
            }
        } catch (Exception e) {
            throw new DataValidationException("Movimiento cuenta " + cuenta
                    + " con monto no valido: " + item.getMonto());
        }

        if (monto.signum() < 0) {
            estado = "ANOMALIA_NEGATIVO";
        }

        MovimientoAnual mov = new MovimientoAnual();
        mov.setCuentaId(cuentaId);
        mov.setFecha(fecha);
        mov.setTransaccion(transaccion);
        mov.setMonto(monto);
        mov.setDescripcion(item.getDescripcion());
        mov.setEstado(estado);
        return mov;
    }
}
