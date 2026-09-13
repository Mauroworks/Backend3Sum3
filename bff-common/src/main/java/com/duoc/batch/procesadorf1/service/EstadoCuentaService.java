package com.duoc.batch.procesadorf1.service;

import com.duoc.batch.procesadorf1.model.EstadoCuentaAnual;
import com.duoc.batch.procesadorf1.model.MovimientoAnual;
import com.duoc.batch.procesadorf1.repository.EstadoCuentaAnualRepository;
import com.duoc.batch.procesadorf1.repository.MovimientoAnualRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Genera y persiste los estados de cuenta anuales por cuenta (Job 3).
 */
@Service
public class EstadoCuentaService {

    private static final Logger log = LoggerFactory.getLogger(EstadoCuentaService.class);

    private final MovimientoAnualRepository movimientoRepository;
    private final EstadoCuentaAnualRepository estadoCuentaRepository;

    public EstadoCuentaService(MovimientoAnualRepository movimientoRepository,
                               EstadoCuentaAnualRepository estadoCuentaRepository) {
        this.movimientoRepository = movimientoRepository;
        this.estadoCuentaRepository = estadoCuentaRepository;
    }

    @Transactional
    public void generarEstadosAnuales() {
        List<MovimientoAnual> movimientos = movimientoRepository.findAll();

        Map<Long, List<MovimientoAnual>> porCuenta = movimientos.stream()
                .collect(Collectors.groupingBy(
                        MovimientoAnual::getCuentaId,
                        LinkedHashMap::new,
                        Collectors.toList()));

        for (Map.Entry<Long, List<MovimientoAnual>> e : porCuenta.entrySet()) {
            Long cuentaId = e.getKey();
            List<MovimientoAnual> lista = e.getValue();
            lista.sort(Comparator.comparing(MovimientoAnual::getFecha));

            BigDecimal depositos = sum(lista, m -> "deposito".equals(m.getTransaccion()));
            BigDecimal retiros = sum(lista, m -> "retiro".equals(m.getTransaccion()));
            BigDecimal compras = sum(lista, m -> "compra".equals(m.getTransaccion()));
            BigDecimal pagos = sum(lista, m -> "pago".equals(m.getTransaccion()));

            // Saldo final = ingresos (depositos + pagos) - egresos (retiros + compras)
            BigDecimal saldoFinal = depositos.add(pagos).subtract(retiros.add(compras));

            EstadoCuentaAnual estado = new EstadoCuentaAnual();
            estado.setCuentaId(cuentaId);
            estado.setTotalMovimientos((long) lista.size());
            estado.setTotalDepositos(depositos);
            estado.setTotalRetiros(retiros);
            estado.setTotalCompras(compras);
            estado.setTotalPagos(pagos);
            estado.setSaldoFinal(saldoFinal);
            estado.setAnio(2024);
            estado.setFechaGeneracion(LocalDate.now());

            estadoCuentaRepository.save(estado);

            log.info("Cuenta {} -> movimientos={} depositos={} retiros={} compras={} "
                            + "pagos={} saldoFinal={}",
                    cuentaId, lista.size(), depositos, retiros, compras, pagos, saldoFinal);
        }
    }

    private BigDecimal sum(List<MovimientoAnual> lista,
                           Function<MovimientoAnual, Boolean> filtro) {
        return lista.stream()
                .filter(filtro::apply)
                .map(MovimientoAnual::getMonto)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
