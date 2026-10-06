package com.duoc.reporting.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "reporte")
public class Reporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sagaId;
    private String tipo;
    private String estado;
    private Integer totalCuentas;
    private BigDecimal saldoTotal;
    private String detalle;

    public Reporte() {
    }

    public Reporte(String sagaId, String tipo, String estado, Integer totalCuentas,
                   BigDecimal saldoTotal, String detalle) {
        this.sagaId = sagaId;
        this.tipo = tipo;
        this.estado = estado;
        this.totalCuentas = totalCuentas;
        this.saldoTotal = saldoTotal;
        this.detalle = detalle;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSagaId() {
        return sagaId;
    }

    public void setSagaId(String sagaId) {
        this.sagaId = sagaId;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Integer getTotalCuentas() {
        return totalCuentas;
    }

    public void setTotalCuentas(Integer totalCuentas) {
        this.totalCuentas = totalCuentas;
    }

    public BigDecimal getSaldoTotal() {
        return saldoTotal;
    }

    public void setSaldoTotal(BigDecimal saldoTotal) {
        this.saldoTotal = saldoTotal;
    }

    public String getDetalle() {
        return detalle;
    }

    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }
}