package com.duoc.batch.procesadorf1.repository;

import com.duoc.batch.procesadorf1.model.CuentaInteres;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CuentaInteresRepository extends JpaRepository<CuentaInteres, Long> {

    List<CuentaInteres> findAllByCuentaId(Long cuentaId);
}
