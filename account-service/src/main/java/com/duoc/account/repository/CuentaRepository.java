package com.duoc.account.repository;

import com.duoc.account.model.Cuenta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CuentaRepository extends JpaRepository<Cuenta, Long> {

    List<Cuenta> findBySagaIdOrderByCuentaIdAsc(String sagaId);

    Optional<Cuenta> findBySagaIdAndCuentaId(String sagaId, Long cuentaId);
}