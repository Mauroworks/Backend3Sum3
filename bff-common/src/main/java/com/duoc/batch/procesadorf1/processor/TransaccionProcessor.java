package com.duoc.batch.procesadorf1.processor;

import com.duoc.batch.procesadorf1.exception.DataValidationException;
import com.duoc.batch.procesadorf1.model.RawTransaccion;
import com.duoc.batch.procesadorf1.model.Transaccion;
import com.duoc.batch.procesadorf1.util.DateParser;
import org.springframework.batch.infrastructure.item.ItemProcessor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Valida y transforma las transacciones crudas.
 *
 * Reglas de consistencia:
 *  - fecha: debe ser una fecha valida (varios formatos). Si es invalida se rechaza.
 *  - monto: debe ser numerico. Si falta o no es numerico se rechaza.
 *  - tipo: debe ser 'credito' o 'debito'. Cualquier otro valor
 *          ("invalid", "desconocido", "-1", etc.) se rechaza.
 *  - Anomalias detectadas: montos negativos o cero se marcan como anomalia
 *    (pero se conservan para el reporte), en vez de descartarse.
 */
public class TransaccionProcessor implements ItemProcessor<RawTransaccion, Transaccion> {

    public static final String CREDITO = "credito";
    public static final String DEBITO = "debito";

    @Override
    public Transaccion process(RawTransaccion item) {
        if (item == null) {
            return null;
        }

        // --- id ---
        Long id;
        try {
            id = Long.valueOf(item.getId().trim());
        } catch (Exception e) {
            throw new DataValidationException("Transaccion con id invalido: " + item.getId());
        }

        // --- fecha ---
        LocalDate fecha = DateParser.parse(item.getFecha());
        if (fecha == null) {
            throw new DataValidationException(
                    "Transaccion " + id + " con fecha no valida: " + item.getFecha());
        }

        // --- monto ---
        BigDecimal monto;
        try {
            monto = new BigDecimal(item.getMonto().trim());
        } catch (Exception e) {
            throw new DataValidationException(
                    "Transaccion " + id + " con monto no valido: " + item.getMonto());
        }

        // --- tipo ---
        String tipo = item.getTipo() == null ? "" : item.getTipo().trim().toLowerCase();
        String estado = "OK";
        if (!CREDITO.equals(tipo) && !DEBITO.equals(tipo)) {
            throw new DataValidationException(
                    "Transaccion " + id + " con tipo no valido: " + item.getTipo());
        }

        // --- deteccion de anomalias ---
        if (monto.signum() < 0) {
            estado = "ANOMALIA_NEGATIVO";
        } else if (monto.signum() == 0) {
            estado = "ANOMALIA_CERO";
        }

        return new Transaccion(id, fecha, monto, tipo, estado);
    }
}
