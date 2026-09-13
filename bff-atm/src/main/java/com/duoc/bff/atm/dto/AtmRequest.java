package com.duoc.bff.atm.dto;

import java.math.BigDecimal;

/**
 * Cuerpo de las peticiones del cajero: identificador de cuenta, PIN y monto
 * (solo para retiros).
 */
public class AtmRequest {

    private Long cuentaId;
    private String pin;
    private BigDecimal monto;

    public Long getCuentaId() {
        return cuentaId;
    }

    public void setCuentaId(Long cuentaId) {
        this.cuentaId = cuentaId;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }
}