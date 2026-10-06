package com.duoc.account.service;

import com.duoc.account.model.Cuenta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Aplica las tasas de interes mensual del Banco XYZ.
 * Reglas heredadas del proceso batch legacy:
 *   ahorro 1.5%, prestamo 2.5%, hipoteca 1.2%.
 */
@Service
public class InteresService {

    private static final Logger log = LoggerFactory.getLogger(InteresService.class);

    private static final Map<String, BigDecimal> TASAS = new LinkedHashMap<>();

    static {
        TASAS.put("ahorro", new BigDecimal("0.015"));
        TASAS.put("prestamo", new BigDecimal("0.025"));
        TASAS.put("hipoteca", new BigDecimal("0.012"));
    }

    /** Tipos no reconocidos se tratan como ahorro y se deja traza. */
    public BigDecimal tasaPara(String tipo) {
        String clave = tipo == null ? "" : tipo.trim().toLowerCase();
        BigDecimal tasa = TASAS.get(clave);
        if (tasa == null) {
            log.warn("[INTERES] tipo desconocido '{}', se aplica tasa de ahorro por defecto", tipo);
            return TASAS.get("ahorro");
        }
        return tasa;
    }

    /** Calcula el saldo final aplicando el interes, redondeando a 2 decimales. */
    public BigDecimal calcularSaldoFinal(BigDecimal saldoBase, BigDecimal tasa) {
        if (saldoBase == null) {
            return BigDecimal.ZERO;
        }
        return saldoBase.add(saldoBase.multiply(tasa)).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calcularInteres(BigDecimal saldoBase, BigDecimal tasa) {
        if (saldoBase == null) {
            return BigDecimal.ZERO;
        }
        return saldoBase.multiply(tasa).setScale(2, RoundingMode.HALF_UP);
    }

    public void registrar(Cuenta cuenta) {
        log.info("[INTERES] cuenta={} tipo={} saldoBase={} tasa={} saldoFinal={}",
                cuenta.getCuentaId(), cuenta.getTipo(), cuenta.getSaldoBase(),
                cuenta.getTasa(), cuenta.getSaldoFinal());
    }
}