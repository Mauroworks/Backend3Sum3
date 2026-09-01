package com.duoc.batch.procesadorf1.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Resultado del calculo de interes mensual para una cuenta (Job 2).
 */
@Entity
@Table(name = "cuenta_interes")
public class CuentaInteres {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cuenta_id")
    private Long cuentaId;

    @Column(length = 60)
    private String nombre;

    @Column(name = "saldo_inicial", precision = 14, scale = 2)
    private BigDecimal saldoInicial;

    private Integer edad;

    @Column(length = 20)
    private String tipo;

    @Column(name = "tasa_interes", precision = 8, scale = 4)
    private BigDecimal tasaInteres;

    @Column(name = "interes_calculado", precision = 14, scale = 2)
    private BigDecimal interesCalculado;

    @Column(name = "saldo_final", precision = 14, scale = 2)
    private BigDecimal saldoFinal;

    @Column(name = "fecha_aplicacion")
    private LocalDate fechaAplicacion;

    @Column(length = 40)
    private String estado;

    public CuentaInteres() {
    }

    public void calcularInteres() {
        if (saldoInicial == null) {
            saldoInicial = BigDecimal.ZERO;
        }
        if (tasaInteres == null) {
            tasaInteres = BigDecimal.ZERO;
        }
        // Interes = saldo * (tasa / 100)
        interesCalculado = saldoInicial
                .multiply(tasaInteres)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        saldoFinal = saldoInicial.add(interesCalculado).setScale(2, RoundingMode.HALF_UP);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public BigDecimal getSaldoInicial() {
        return saldoInicial;
    }

    public void setSaldoInicial(BigDecimal saldoInicial) {
        this.saldoInicial = saldoInicial;
    }

    public Integer getEdad() {
        return edad;
    }

    public void setEdad(Integer edad) {
        this.edad = edad;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public BigDecimal getTasaInteres() {
        return tasaInteres;
    }

    public void setTasaInteres(BigDecimal tasaInteres) {
        this.tasaInteres = tasaInteres;
    }

    public BigDecimal getInteresCalculado() {
        return interesCalculado;
    }

    public void setInteresCalculado(BigDecimal interesCalculado) {
        this.interesCalculado = interesCalculado;
    }

    public BigDecimal getSaldoFinal() {
        return saldoFinal;
    }

    public void setSaldoFinal(BigDecimal saldoFinal) {
        this.saldoFinal = saldoFinal;
    }

    public LocalDate getFechaAplicacion() {
        return fechaAplicacion;
    }

    public void setFechaAplicacion(LocalDate fechaAplicacion) {
        this.fechaAplicacion = fechaAplicacion;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
