package com.duoc.batch.procesadorf1.service;

import com.duoc.batch.procesadorf1.model.ResumenDiario;
import com.duoc.batch.procesadorf1.processor.TransaccionProcessor;
import com.duoc.batch.procesadorf1.repository.ResumenDiarioRepository;
import com.duoc.batch.procesadorf1.repository.TransaccionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Genera y persiste el resumen del reporte de transacciones diarias (Job 1).
 */
@Service
public class ReporteService {

    private static final Logger log = LoggerFactory.getLogger(ReporteService.class);

    private final TransaccionRepository transaccionRepository;
    private final ResumenDiarioRepository resumenDiarioRepository;

    public ReporteService(TransaccionRepository transaccionRepository,
                          ResumenDiarioRepository resumenDiarioRepository) {
        this.transaccionRepository = transaccionRepository;
        this.resumenDiarioRepository = resumenDiarioRepository;
    }

    @Transactional
    public ResumenDiario generarResumenDiario() {
        long total = transaccionRepository.count();
        long creditos = transaccionRepository.countByTipo(TransaccionProcessor.CREDITO);
        long debitos = transaccionRepository.countByTipo(TransaccionProcessor.DEBITO);

        BigDecimal montoCreditos =
                transaccionRepository.sumMontoByTipo(TransaccionProcessor.CREDITO);
        BigDecimal montoDebitos =
                transaccionRepository.sumMontoByTipo(TransaccionProcessor.DEBITO);

        // anomalias: registros procesados cuyo estado no es "OK"
        long anomalias = transaccionRepository.findAll().stream()
                .filter(t -> !"OK".equals(t.getEstado()))
                .count();

        ResumenDiario resumen = new ResumenDiario();
        resumen.setFechaProceso(LocalDateTime.now());
        resumen.setTotalTransacciones(total);
        resumen.setTotalCreditos(creditos);
        resumen.setTotalDebitos(debitos);
        resumen.setMontoTotalCreditos(montoCreditos);
        resumen.setMontoTotalDebitos(montoDebitos);
        resumen.setMontoTotal(montoCreditos.add(montoDebitos));
        resumen.setTotalAnomalias(anomalias);

        ResumenDiario saved = resumenDiarioRepository.save(resumen);

        log.info("===== RESUMEN DE TRANSACCIONES DIARIAS =====");
        log.info("Total transacciones : {}", total);
        log.info("Creditos   : {} (monto {})", creditos, montoCreditos);
        log.info("Debitos    : {} (monto {})", debitos, montoDebitos);
        log.info("Monto total: {}", resumen.getMontoTotal());
        log.info("Anomalias detectadas: {}", anomalias);
        log.info("============================================");
        return saved;
    }
}
