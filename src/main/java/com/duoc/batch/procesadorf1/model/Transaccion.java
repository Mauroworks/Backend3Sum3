package com.duoc.batch.procesadorf1.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Transaccion procesada y validada a partir de transacciones.csv.
 */
@Entity
@Table(name = "transaccion")
public class Transaccion {

    @Id
    private Long id;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false)
    private BigDecimal monto;

    @Column(nullable = false, length = 20)
    private String tipo;

    /**
     * Estado de la anomalia detectada: OK, ANOMALIA_NEGATIVO,
     * ANOMALIA_CERO u OK_CORREGIDO.
     */
    @Column(length = 40)
    private String estado;

    public Transaccion() {
    }

    public Transaccion(Long id, LocalDate fecha, BigDecimal monto, String tipo, String estado) {
        this.id = id;
        this.fecha = fecha;
        this.monto = monto;
        this.tipo = tipo;
        this.estado = estado;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
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
}
