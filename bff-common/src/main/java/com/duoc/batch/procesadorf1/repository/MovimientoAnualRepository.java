package com.duoc.batch.procesadorf1.repository;

import com.duoc.batch.procesadorf1.model.MovimientoAnual;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovimientoAnualRepository extends JpaRepository<MovimientoAnual, Long> {

    List<MovimientoAnual> findAllByCuentaIdOrderByFechaDesc(Long cuentaId);
}
