package com.duoc.csvresult.repository;

import com.duoc.csvresult.model.ResultadoMigracion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResultadoMigracionRepository extends JpaRepository<ResultadoMigracion, Long> {

    List<ResultadoMigracion> findBySagaIdOrderByIdAsc(String sagaId);

    List<ResultadoMigracion> findByArchivoOrigen(String archivoOrigen);

    long countByEstado(String estado);
}