package com.duoc.csvresult.service;

import com.duoc.csvresult.model.ResultadoMigracion;
import com.duoc.csvresult.repository.ResultadoMigracionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ResultadoMigracionService {

    private final ResultadoMigracionRepository repository;

    public ResultadoMigracionService(ResultadoMigracionRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ResultadoMigracion> porSaga(String sagaId) {
        return repository.findBySagaIdOrderByIdAsc(sagaId);
    }

    @Transactional(readOnly = true)
    public List<ResultadoMigracion> todos() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> resumen() {
        List<ResultadoMigracion> todos = repository.findAll();
        BigDecimal totalMonto = todos.stream()
                .filter(r -> !"RECHAZADO".equals(r.getEstado()))
                .map(ResultadoMigracion::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("totalFilas", todos.size());
        mapa.put("migrados", repository.countByEstado("MIGRADO"));
        mapa.put("migradosConAnomalia", repository.countByEstado("MIGRADO_CON_ANOMALIA"));
        mapa.put("rechazados", repository.countByEstado("RECHAZADO"));
        mapa.put("totalMonto", totalMonto);
        return mapa;
    }
}