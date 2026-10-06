package com.duoc.account.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "cuenta")
public class Cuenta {

    @Id
    private Long cuentaId;

    private String sagaId;
    private String nombre;
    private String tipo;
    private BigDecimal saldoBase;
    private BigDecimal tasa;
    private BigDecimal saldoFinal;

    public Cuenta() {
    }

    public Cuenta(Long cuentaId, String sagaId, String nombre, String tipo,
                  BigDecimal saldoBase, BigDecimal tasa, BigDecimal saldoFinal) {
        this.cuentaId = cuentaId;
        this.sagaId = sagaId;
        this.nombre = nombre;
        this.tipo = tipo;
        this.saldoBase = saldoBase;
        this.tasa = tasa;
        this.saldoFinal = saldoFinal;
    }

    public Long getCuentaId() {
        return cuentaId;
    }

    public void setCuentaId(Long cuentaId) {
        this.cuentaId = cuentaId;
    }

    public String getSagaId() {
        return sagaId;
    }

    public void setSagaId(String sagaId) {
        this.sagaId = sagaId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public BigDecimal getSaldoBase() {
        return saldoBase;
    }

    public void setSaldoBase(BigDecimal saldoBase) {
        this.saldoBase = saldoBase;
    }

    public BigDecimal getTasa() {
        return tasa;
    }

    public void setTasa(BigDecimal tasa) {
        this.tasa = tasa;
    }

    public BigDecimal getSaldoFinal() {
        return saldoFinal;
    }

    public void setSaldoFinal(BigDecimal saldoFinal) {
        this.saldoFinal = saldoFinal;
    }
}