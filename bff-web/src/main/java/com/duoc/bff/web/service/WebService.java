package com.duoc.bff.web.service;

import com.duoc.batch.procesadorf1.model.CuentaInteres;
import com.duoc.batch.procesadorf1.model.CuentaSaldoFinal;
import com.duoc.batch.procesadorf1.model.EstadoCuentaAnual;
import com.duoc.batch.procesadorf1.model.MovimientoAnual;
import com.duoc.batch.procesadorf1.model.ResumenDiario;
import com.duoc.batch.procesadorf1.model.Transaccion;
import com.duoc.batch.procesadorf1.repository.CuentaInteresRepository;
import com.duoc.batch.procesadorf1.repository.CuentaSaldoFinalRepository;
import com.duoc.batch.procesadorf1.repository.EstadoCuentaAnualRepository;
import com.duoc.batch.procesadorf1.repository.MovimientoAnualRepository;
import com.duoc.batch.procesadorf1.repository.ResumenDiarioRepository;
import com.duoc.batch.procesadorf1.repository.TransaccionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Servicio del BFF Web: entrega datos completos y enriquecidos para
 * interfaces de navegador (pantallas complejas, dashboard y auditorias).
 */
@Service
public class WebService {

    private final ResumenDiarioRepository resumenDiarioRepository;
    private final TransaccionRepository transaccionRepository;
    private final CuentaSaldoFinalRepository cuentaSaldoFinalRepository;
    private final CuentaInteresRepository cuentaInteresRepository;
    private final EstadoCuentaAnualRepository estadoCuentaAnualRepository;
    private final MovimientoAnualRepository movimientoAnualRepository;

    public WebService(ResumenDiarioRepository resumenDiarioRepository,
                      TransaccionRepository transaccionRepository,
                      CuentaSaldoFinalRepository cuentaSaldoFinalRepository,
                      CuentaInteresRepository cuentaInteresRepository,
                      EstadoCuentaAnualRepository estadoCuentaAnualRepository,
                      MovimientoAnualRepository movimientoAnualRepository) {
        this.resumenDiarioRepository = resumenDiarioRepository;
        this.transaccionRepository = transaccionRepository;
        this.cuentaSaldoFinalRepository = cuentaSaldoFinalRepository;
        this.cuentaInteresRepository = cuentaInteresRepository;
        this.estadoCuentaAnualRepository = estadoCuentaAnualRepository;
        this.movimientoAnualRepository = movimientoAnualRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> dashboard() {
        Map<String, Object> dash = new LinkedHashMap<>();
        ResumenDiario resumen = resumenDiarioRepository.findAll().stream()
                .max(Comparator.comparing(ResumenDiario::getId))
                .orElse(null);
        dash.put("resumen_diario", resumen);
        dash.put("total_cuentas", cuentaSaldoFinalRepository.count());
        dash.put("movimientos_anuales", movimientoAnualRepository.count());
        return dash;
    }

    @Transactional(readOnly = true)
    public List<Transaccion> todasLasTransacciones() {
        return transaccionRepository.findAll().stream()
                .sorted(Comparator.comparing(Transaccion::getId))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<Transaccion> transaccionPorId(Long id) {
        return transaccionRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<CuentaSaldoFinal> todasLasCuentas() {
        return cuentaSaldoFinalRepository.findAll().stream()
                .sorted(Comparator.comparing(CuentaSaldoFinal::getCuentaId))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> detalleCuenta(Long cuentaId) {
        Map<String, Object> detalle = new LinkedHashMap<>();
        CuentaSaldoFinal saldo = cuentaSaldoFinalRepository.findById(cuentaId).orElse(null);
        if (saldo == null) {
            return null;
        }
        List<CuentaInteres> intereses = cuentaInteresRepository.findAllByCuentaId(cuentaId);
        EstadoCuentaAnual estado = estadoCuentaAnualRepository.findById(cuentaId).orElse(null);
        detalle.put("saldo", saldo);
        detalle.put("intereses", intereses);
        detalle.put("estado_cuenta_anual", estado);
        return detalle;
    }

    @Transactional(readOnly = true)
    public List<EstadoCuentaAnual> estadosDeCuenta() {
        return estadoCuentaAnualRepository.findAll().stream()
                .sorted(Comparator.comparing(EstadoCuentaAnual::getCuentaId))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MovimientoAnual> movimientosDeCuenta(Long cuentaId) {
        return movimientoAnualRepository.findAllByCuentaIdOrderByFechaDesc(cuentaId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> estadisticas() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("por_tipo", transaccionRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        Transaccion::getTipo, Collectors.counting())));
        stats.put("por_estado", transaccionRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        Transaccion::getEstado, Collectors.counting())));
        stats.put("por_transaccion_cuenta_anual", movimientoAnualRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        MovimientoAnual::getTransaccion, Collectors.counting())));
        return stats;
    }
}