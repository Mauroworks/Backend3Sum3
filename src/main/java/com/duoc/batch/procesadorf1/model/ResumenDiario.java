package com.duoc.batch.procesadorf1.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Resumen del reporte de transacciones diarias (Job 1).
 */
@Entity
@Table(name = "resumen_diario")
public class ResumenDiario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha_proceso")
    private LocalDateTime fechaProceso;

    @Column(name = "total_transacciones")
    private Long totalTransacciones;

    @Column(name = "total_creditos")
    private Long totalCreditos;

    @Column(name = "total_debitos")
    private Long totalDebitos;

    @Column(name = "monto_total_creditos")
    private BigDecimal montoTotalCreditos;

    @Column(name = "monto_total_debitos")
    private BigDecimal montoTotalDebitos;

    @Column(name = "monto_total")
    private BigDecimal montoTotal;

    @Column(name = "total_anomalias")
    private Long totalAnomalias;

    public ResumenDiario() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getFechaProceso() {
        return fechaProceso;
    }

    public void setFechaProceso(LocalDateTime fechaProceso) {
        this.fechaProceso = fechaProceso;
    }

    public Long getTotalTransacciones() {
        return totalTransacciones;
    }

    public void setTotalTransacciones(Long totalTransacciones) {
        this.totalTransacciones = totalTransacciones;
    }

    public Long getTotalCreditos() {
        return totalCreditos;
    }

    public void setTotalCreditos(Long totalCreditos) {
        this.totalCreditos = totalCreditos;
    }

    public Long getTotalDebitos() {
        return totalDebitos;
    }

    public void setTotalDebitos(Long totalDebitos) {
        this.totalDebitos = totalDebitos;
    }

    public BigDecimal getMontoTotalCreditos() {
        return montoTotalCreditos;
    }

    public void setMontoTotalCreditos(BigDecimal montoTotalCreditos) {
        this.montoTotalCreditos = montoTotalCreditos;
    }

    public BigDecimal getMontoTotalDebitos() {
        return montoTotalDebitos;
    }

    public void setMontoTotalDebitos(BigDecimal montoTotalDebitos) {
        this.montoTotalDebitos = montoTotalDebitos;
    }

    public BigDecimal getMontoTotal() {
        return montoTotal;
    }

    public void setMontoTotal(BigDecimal montoTotal) {
        this.montoTotal = montoTotal;
    }

    public Long getTotalAnomalias() {
        return totalAnomalias;
    }

    public void setTotalAnomalias(Long totalAnomalias) {
        this.totalAnomalias = totalAnomalias;
    }
}
