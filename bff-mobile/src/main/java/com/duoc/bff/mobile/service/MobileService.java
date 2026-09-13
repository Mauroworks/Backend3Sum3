package com.duoc.bff.mobile.service;

import com.duoc.batch.procesadorf1.model.CuentaSaldoFinal;
import com.duoc.batch.procesadorf1.model.MovimientoAnual;
import com.duoc.batch.procesadorf1.model.ResumenDiario;
import com.duoc.batch.procesadorf1.model.Transaccion;
import com.duoc.batch.procesadorf1.repository.CuentaSaldoFinalRepository;
import com.duoc.batch.procesadorf1.repository.MovimientoAnualRepository;
import com.duoc.batch.procesadorf1.repository.ResumenDiarioRepository;
import com.duoc.batch.procesadorf1.repository.TransaccionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio del BFF Movil: entrega payloads ligeros (sin descripciones, sin
 * campos de auditoria) para minimizar el ancho de banda en el movil.
 */
@Service
public class MobileService {

    private final ResumenDiarioRepository resumenDiarioRepository;
    private final TransaccionRepository transaccionRepository;
    private final CuentaSaldoFinalRepository cuentaSaldoFinalRepository;
    private final MovimientoAnualRepository movimientoAnualRepository;

    public MobileService(ResumenDiarioRepository resumenDiarioRepository,
                         TransaccionRepository transaccionRepository,
                         CuentaSaldoFinalRepository cuentaSaldoFinalRepository,
                         MovimientoAnualRepository movimientoAnualRepository) {
        this.resumenDiarioRepository = resumenDiarioRepository;
        this.transaccionRepository = transaccionRepository;
        this.cuentaSaldoFinalRepository = cuentaSaldoFinalRepository;
        this.movimientoAnualRepository = movimientoAnualRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> resumen() {
        ResumenDiario r = resumenDiarioRepository.findAll().stream()
                .max(Comparator.comparing(ResumenDiario::getId))
                .orElse(null);
        Map<String, Object> map = new LinkedHashMap<>();
        if (r == null) {
            map.put("total", 0);
            map.put("creditos", 0);
            map.put("debitos", 0);
            map.put("monto_total", 0);
            map.put("anomalias", 0);
        } else {
            map.put("total", r.getTotalTransacciones());
            map.put("creditos", r.getTotalCreditos());
            map.put("debitos", r.getTotalDebitos());
            map.put("monto_total", r.getMontoTotal());
            map.put("anomalias", r.getTotalAnomalias());
        }
        return map;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> saldos() {
        return cuentaSaldoFinalRepository.findAll().stream()
                .sorted(Comparator.comparing(CuentaSaldoFinal::getCuentaId))
                .map(this::toSaldoLigero)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> transacciones(int limit) {
        return transaccionRepository.findAll().stream()
                .sorted(Comparator.comparing(Transaccion::getId).reversed())
                .limit(limit)
                .map(this::toTransaccionLigera)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> resumenCuenta(Long cuentaId) {
        Map<String, Object> map = new LinkedHashMap<>();
        CuentaSaldoFinal saldo = cuentaSaldoFinalRepository.findById(cuentaId).orElse(null);
        if (saldo == null) {
            return null;
        }
        map.put("cuenta_id", saldo.getCuentaId());
        map.put("saldo", saldo.getSaldo());
        map.put("interes_acumulado", saldo.getInteresAcumulado());
        map.put("movimientos", movimientos(cuentaId, 10));
        return map;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> movimientos(Long cuentaId, int limit) {
        return movimientoAnualRepository.findAllByCuentaIdOrderByFechaDesc(cuentaId).stream()
                .limit(limit)
                .map(this::toMovimientoLigero)
                .collect(Collectors.toList());
    }

    private Map<String, Object> toSaldoLigero(CuentaSaldoFinal c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("cuenta_id", c.getCuentaId());
        m.put("saldo", c.getSaldo());
        m.put("tipo", c.getTipo());
        return m;
    }

    private Map<String, Object> toTransaccionLigera(Transaccion t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("fecha", String.valueOf(t.getFecha()));
        m.put("monto", t.getMonto());
        m.put("tipo", t.getTipo());
        return m;
    }

    private Map<String, Object> toMovimientoLigero(MovimientoAnual m) {
        Map<String, Object> x = new LinkedHashMap<>();
        x.put("fecha", String.valueOf(m.getFecha()));
        x.put("tipo", m.getTransaccion());
        x.put("monto", m.getMonto());
        return x;
    }
}