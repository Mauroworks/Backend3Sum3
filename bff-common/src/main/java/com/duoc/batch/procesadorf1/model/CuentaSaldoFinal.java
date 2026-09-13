package com.duoc.batch.procesadorf1.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Saldo final de la cuenta luego de aplicar el interes mensual (Job 2).
 * Se acumula el saldo tras el interes de cada registro de intereses.csv.
 */
@Entity
@Table(name = "cuenta_saldo_final")
public class CuentaSaldoFinal {

    @Id
    @Column(name = "cuenta_id")
    private Long cuentaId;

    @Column(length = 60)
    private String nombre;

    @Column(precision = 14, scale = 2)
    private BigDecimal saldo;

    @Column(length = 20)
    private String tipo;

    @Column(name = "interes_acumulado", precision = 14, scale = 2)
    private BigDecimal interesAcumulado;

    @Column(name = "fecha_actualizacion")
    private LocalDate fechaActualizacion;

    public CuentaSaldoFinal() {
    }

    public Long getCuentaId() {
        return cuentaId;
    }

    public void setCuentaId(Long cuentaId) {
        this.cuentaId = cuentaId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public BigDecimal getSaldo() {
        return saldo;
    }

    public void setSaldo(BigDecimal saldo) {
        this.saldo = saldo;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public BigDecimal getInteresAcumulado() {
        return interesAcumulado;
    }

    public void setInteresAcumulado(BigDecimal interesAcumulado) {
        this.interesAcumulado = interesAcumulado;
    }

    public LocalDate getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(LocalDate fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }
}
