package com.duoc.batch.procesadorf1.processor;

import com.duoc.batch.procesadorf1.exception.DataValidationException;
import com.duoc.batch.procesadorf1.model.CuentaInteres;
import com.duoc.batch.procesadorf1.model.RawInteres;
import org.springframework.batch.infrastructure.item.ItemProcessor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Valida y transforma los registros de intereses, aplicando correcciones
 * cuando los datos estan incompletos o fuera de rango.
 *
 * Tipos validos y sus tasas mensuales:
 *  - ahorro  : 1.5%
 *  - prestamo: 2.5%
 *  - hipoteca: 1.2%
 *
 * Correcciones:
 *  - saldo vacio    -> 0 (CORREGIDO)
 *  - edad vacia     -> 0 (CORREGIDO), fuera de rango [0,120] -> 0 (CORREGIDO)
 *  - nombre vacio   -> "SIN_NOMBRE" (CORREGIDO)
 *
 * Rechazos (skip):
 *  - tipo desconocido ("-1", "unknown", vacio, etc.)
 *  - cuenta_id no numerico
 *  - saldo o edad no numericos
 */
public class InteresProcessor implements ItemProcessor<RawInteres, CuentaInteres> {

    public static final String AHORRO = "ahorro";
    public static final String PRESTAMO = "prestamo";
    public static final String HIPOTECA = "hipoteca";

    private static final int EDAD_MIN = 0;
    private static final int EDAD_MAX = 120;

    private static final Map<String, BigDecimal> TASAS = new HashMap<>();

    static {
        TASAS.put(AHORRO, new BigDecimal("1.5"));
        TASAS.put(PRESTAMO, new BigDecimal("2.5"));
        TASAS.put(HIPOTECA, new BigDecimal("1.2"));
    }

    @Override
    public CuentaInteres process(RawInteres item) {
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

        // --- tipo (debe ser valido, si no se rechaza) ---
        String tipo = item.getTipo() == null ? "" : item.getTipo().trim().toLowerCase();
        BigDecimal tasa = TASAS.get(tipo);
        if (tasa == null) {
            throw new DataValidationException(
                    "Cuenta " + cuentaId + " con tipo no valido: " + item.getTipo());
        }

        // --- saldo (correccion si vacio) ---
        BigDecimal saldo;
        try {
            saldo = (item.getSaldo() == null || item.getSaldo().isBlank())
                    ? BigDecimal.ZERO
                    : new BigDecimal(item.getSaldo().trim());
        } catch (Exception e) {
            throw new DataValidationException(
                    "Cuenta " + cuentaId + " con saldo no valido: " + item.getSaldo());
        }

        // --- edad (correccion si vacia o fuera de rango) ---
        Integer edad;
        boolean edadCorregida = false;
        try {
            if (item.getEdad() == null || item.getEdad().isBlank()) {
                edad = 0;
                edadCorregida = true;
            } else {
                int parsed = Integer.parseInt(item.getEdad().trim());
                if (parsed >= EDAD_MIN && parsed <= EDAD_MAX) {
                    edad = parsed;
                } else {
                    edad = 0;
                    edadCorregida = true;
                }
            }
        } catch (Exception e) {
            throw new DataValidationException(
                    "Cuenta " + cuentaId + " con edad no valida: " + item.getEdad());
        }

        // --- nombre (correccion si vacio) ---
        String nombre = (item.getNombre() == null || item.getNombre().isBlank())
                ? "SIN_NOMBRE"
                : item.getNombre().trim();

        String estado = "OK";
        if (saldo.signum() == 0) {
            estado = "SALDO_CORREGIDO";
        }
        if (edadCorregida) {
            estado = "EDAD_CORREGIDA";
        }

        CuentaInteres cuenta = new CuentaInteres();
        cuenta.setCuentaId(cuentaId);
        cuenta.setNombre(nombre);
        cuenta.setSaldoInicial(saldo);
        cuenta.setEdad(edad);
        cuenta.setTipo(tipo);
        cuenta.setTasaInteres(tasa);
        cuenta.setFechaAplicacion(LocalDate.now());
        cuenta.setEstado(estado);
        cuenta.calcularInteres();
        return cuenta;
    }
}
