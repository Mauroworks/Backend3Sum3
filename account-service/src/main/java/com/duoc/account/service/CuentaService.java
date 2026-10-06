package com.duoc.account.service;

import com.duoc.account.model.Cuenta;
import com.duoc.account.repository.CuentaRepository;
import com.duoc.saga.contracts.InteresesCalculadosEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CuentaService {

    private static final Logger log = LoggerFactory.getLogger(CuentaService.class);

    private final CuentaRepository repository;
    private final InteresService interesService;

    public CuentaService(CuentaRepository repository, InteresService interesService) {
        this.repository = repository;
        this.interesService = interesService;
    }

    /**
     * Aplica el interes mensual y persiste el saldo final por cuenta.
     * Segundo paso de la Saga coreografica.
     */
    @Transactional
    public InteresesCalculadosEvent calcular(String sagaId, List<InteresesCalculadosEvent.CuentaConInteres> cuentas) {
        List<InteresesCalculadosEvent.CuentaConInteres> resultado = cuentas.stream()
                .map(c -> {
                    BigDecimal tasa = interesService.tasaPara(c.tipo());
                    BigDecimal saldoFinal = interesService.calcularSaldoFinal(c.saldoBase(), tasa);
                    Cuenta cuenta = new Cuenta(c.cuentaId(), sagaId, "CLIENTE-" + c.cuentaId(),
                            c.tipo(), c.saldoBase(), tasa, saldoFinal);
                    repository.save(cuenta);
                    interesService.registrar(cuenta);
                    return new InteresesCalculadosEvent.CuentaConInteres(c.cuentaId(), c.tipo(),
                            c.saldoBase(), tasa, saldoFinal);
                })
                .toList();

        BigDecimal total = resultado.stream()
                .map(c -> c.saldoFinal().subtract(c.saldoBase()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("[SAGA-PASO] sagaId={} cuentas={} totalInteres={}", sagaId, resultado.size(), total);
        return InteresesCalculadosEvent.of(sagaId, resultado, total);
    }

    @Transactional(readOnly = true)
    public List<Cuenta> porSaga(String sagaId) {
        return repository.findBySagaIdOrderByCuentaIdAsc(sagaId);
    }

    @Transactional(readOnly = true)
    public List<Cuenta> todas() {
        return repository.findAll();
    }
}