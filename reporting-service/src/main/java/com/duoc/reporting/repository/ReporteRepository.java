package com.duoc.reporting.repository;

import com.duoc.reporting.model.Reporte;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReporteRepository extends JpaRepository<Reporte, Long> {

    List<Reporte> findBySagaIdOrderByIdAsc(String sagaId);
}