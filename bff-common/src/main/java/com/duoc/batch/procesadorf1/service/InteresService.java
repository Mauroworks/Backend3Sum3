package com.duoc.batch.procesadorf1.service;

import com.duoc.batch.procesadorf1.model.CuentaInteres;
import com.duoc.batch.procesadorf1.model.CuentaSaldoFinal;
import com.duoc.batch.procesadorf1.repository.CuentaInteresRepository;
import com.duoc.batch.procesadorf1.repository.CuentaSaldoFinalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Actualiza el saldo final de las cuentas en la base de datos luego de
 * aplicar el interes mensual calculado (Job 2).
 */
@Service
public class InteresService {

    private static final Logger log = LoggerFactory.getLogger(InteresService.class);

    private final CuentaInteresRepository cuentaInteresRepository;
    private final CuentaSaldoFinalRepository saldoFinalRepository;

    public InteresService(CuentaInteresRepository cuentaInteresRepository,
                          CuentaSaldoFinalRepository saldoFinalRepository) {
        this.cuentaInteresRepository = cuentaInteresRepository;
        this.saldoFinalRepository = saldoFinalRepository;
    }

    @Transactional
    public void actualizarSaldoFinal() {
        List<CuentaInteres> registros = cuentaInteresRepository.findAll();

        Map<Long, List<CuentaInteres>> porCuenta = registros.stream()
                .collect(Collectors.groupingBy(CuentaInteres::getCuentaId));

        porCuenta.forEach((cuentaId, lista) -> {
            BigDecimal saldo = lista.stream()
                    .map(CuentaInteres::getSaldoFinal)
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal interes = lista.stream()
                    .map(CuentaInteres::getInteresCalculado)
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            CuentaSaldoFinal saldoFinal = new CuentaSaldoFinal();
            saldoFinal.setCuentaId(cuentaId);
            saldoFinal.setNombre(lista.get(0).getNombre());
            saldoFinal.setTipo(lista.get(0).getTipo());
            saldoFinal.setSaldo(saldo);
            saldoFinal.setInteresAcumulado(interes);
            saldoFinal.setFechaActualizacion(LocalDate.now());

            saldoFinalRepository.save(saldoFinal);
            log.info("Cuenta {} ({}) -> saldoFinal={} interesAcumulado={}",
                    cuentaId, lista.get(0).getNombre(), saldo, interes);
        });

        log.info("Total de cuentas actualizadas en BD: {}", porCuenta.size());
    }
}
