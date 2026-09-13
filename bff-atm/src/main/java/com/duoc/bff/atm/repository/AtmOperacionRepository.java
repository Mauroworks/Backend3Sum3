package com.duoc.bff.atm.repository;

import com.duoc.bff.atm.model.AtmOperacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AtmOperacionRepository extends JpaRepository<AtmOperacion, Long> {

    List<AtmOperacion> findAllByCuentaIdOrderByFechaDesc(Long cuentaId);

    List<AtmOperacion> findAllByOrderByFechaDesc();
}