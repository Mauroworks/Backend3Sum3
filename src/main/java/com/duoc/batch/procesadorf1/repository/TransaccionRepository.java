package com.duoc.batch.procesadorf1.repository;

import com.duoc.batch.procesadorf1.model.Transaccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface TransaccionRepository extends JpaRepository<Transaccion, Long> {

    List<Transaccion> findByEstadoNot(String estado);

    long countByTipo(String tipo);

    @Query("select coalesce(sum(t.monto), 0) from Transaccion t where t.tipo = :tipo")
    BigDecimal sumMontoByTipo(@Param("tipo") String tipo);
}
