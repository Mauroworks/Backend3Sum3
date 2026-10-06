package com.duoc.reporting.service;

import com.duoc.reporting.model.Reporte;
import com.duoc.reporting.repository.ReporteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReporteService {

    private static final Logger log = LoggerFactory.getLogger(ReporteService.class);

    private final ReporteRepository repository;

    public ReporteService(ReporteRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Reporte guardar(Reporte reporte) {
        Reporte guardado = repository.save(reporte);
        log.info("[REPORTE] id={} sagaId={} tipo={} totalCuentas={} saldoTotal={}",
                guardado.getId(), guardado.getSagaId(), guardado.getTipo(),
                guardado.getTotalCuentas(), guardado.getSaldoTotal());
        return guardado;
    }

    @Transactional(readOnly = true)
    public List<Reporte> todos() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Reporte> porSaga(String sagaId) {
        return repository.findBySagaIdOrderByIdAsc(sagaId);
    }
}