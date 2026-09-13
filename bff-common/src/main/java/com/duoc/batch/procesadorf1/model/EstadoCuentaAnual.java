package com.duoc.batch.procesadorf1.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Estado de cuenta anual consolidado por cuenta (Job 3), para auditorias.
 */
@Entity
@Table(name = "estado_cuenta_anual")
public class EstadoCuentaAnual {

    @Id
    @Column(name = "cuenta_id")
    private Long cuentaId;

    @Column(name = "total_movimientos")
    private Long totalMovimientos;

    @Column(name = "total_depositos", precision = 14, scale = 2)
    private BigDecimal totalDepositos;

    @Column(name = "total_retiros", precision = 14, scale = 2)
    private BigDecimal totalRetiros;

    @Column(name = "total_compras", precision = 14, scale = 2)
    private BigDecimal totalCompras;

    @Column(name = "total_pagos", precision = 14, scale = 2)
    private BigDecimal totalPagos;

    @Column(name = "saldo_final", precision = 14, scale = 2)
    private BigDecimal saldoFinal;

    @Column(name = "año")
    private Integer anio;

    @Column(name = "fecha_generacion")
    private LocalDate fechaGeneracion;

    public EstadoCuentaAnual() {
    }

    public Long getCuentaId() {
        return cuentaId;
    }

    public void setCuentaId(Long cuentaId) {
        this.cuentaId = cuentaId;
    }

    public Long getTotalMovimientos() {
        return totalMovimientos;
    }

    public void setTotalMovimientos(Long totalMovimientos) {
        this.totalMovimientos = totalMovimientos;
    }

    public BigDecimal getTotalDepositos() {
        return totalDepositos;
    }

    public void setTotalDepositos(BigDecimal totalDepositos) {
        this.totalDepositos = totalDepositos;
    }

    public BigDecimal getTotalRetiros() {
        return totalRetiros;
    }

    public void setTotalRetiros(BigDecimal totalRetiros) {
        this.totalRetiros = totalRetiros;
    }

    public BigDecimal getTotalCompras() {
        return totalCompras;
    }

    public void setTotalCompras(BigDecimal totalCompras) {
        this.totalCompras = totalCompras;
    }

    public BigDecimal getTotalPagos() {
        return totalPagos;
    }

    public void setTotalPagos(BigDecimal totalPagos) {
        this.totalPagos = totalPagos;
    }

    public BigDecimal getSaldoFinal() {
        return saldoFinal;
    }

    public void setSaldoFinal(BigDecimal saldoFinal) {
        this.saldoFinal = saldoFinal;
    }

    public Integer getAnio() {
        return anio;
    }

    public void setAnio(Integer anio) {
        this.anio = anio;
    }

    public LocalDate getFechaGeneracion() {
        return fechaGeneracion;
    }

    public void setFechaGeneracion(LocalDate fechaGeneracion) {
        this.fechaGeneracion = fechaGeneracion;
    }
}
