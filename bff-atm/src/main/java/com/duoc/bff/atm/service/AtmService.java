package com.duoc.bff.atm.service;

import com.duoc.batch.procesadorf1.model.CuentaSaldoFinal;
import com.duoc.batch.procesadorf1.model.Transaccion;
import com.duoc.batch.procesadorf1.repository.CuentaSaldoFinalRepository;
import com.duoc.batch.procesadorf1.repository.TransaccionRepository;
import com.duoc.bff.atm.dto.AtmResult;
import com.duoc.bff.atm.model.AtmOperacion;
import com.duoc.bff.atm.repository.AtmOperacionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Servicio del BFF de Cajeros Automaticos: operaciones criticas (consulta de
 * saldo y retiro) validadas con PIN y registradas en auditoria.
 */
@Service
public class AtmService {

    public static final String SALDO = "SALDO";
    public static final String RETIRO = "RETIRO";

    private final CuentaSaldoFinalRepository saldoRepository;
    private final TransaccionRepository transaccionRepository;
    private final AtmOperacionRepository operacionRepository;

    @Value("${atm.pin.default:1234}")
    private String pinDefault;

    @Value("${atm.cajero.id:ATM-001}")
    private String cajeroId;

    @Value("${atm.cajero.version:1.0.0}")
    private String cajeroVersion;

    public AtmService(CuentaSaldoFinalRepository saldoRepository,
                      TransaccionRepository transaccionRepository,
                      AtmOperacionRepository operacionRepository) {
        this.saldoRepository = saldoRepository;
        this.transaccionRepository = transaccionRepository;
        this.operacionRepository = operacionRepository;
    }

    /** Consulta de saldo: PIN + cuenta valida => saldo en pesos. */
    @Transactional
    public AtmResult consultarSaldo(Long cuentaId, String pin) {
        if (!pinValido(pin)) {
            return fallo(SALDO, cuentaId, null, HttpStatus.UNAUTHORIZED, "PIN_INVALIDO");
        }
        CuentaSaldoFinal cuenta = saldoRepository.findById(cuentaId).orElse(null);
        if (cuenta == null) {
            return fallo(SALDO, cuentaId, null, HttpStatus.NOT_FOUND, "CUENTA_NO_EXISTE");
        }

        AtmResult result = AtmResult.ok(SALDO);
        result.getBody().put("cuenta_id", cuenta.getCuentaId());
        result.getBody().put("nombre", cuenta.getNombre());
        result.getBody().put("saldo", cuenta.getSaldo());
        result.getBody().put("fecha", String.valueOf(LocalDateTime.now()));
        result.getBody().put("ticket", generarTicket(SALDO));

        registrar(SALDO, cuenta.getCuentaId(), null, cuenta.getSaldo(), true, "Aprobada");
        return result;
    }

    /** Retiro: PIN + monto valido + fondos suficientes => debita la cuenta. */
    @Transactional
    public AtmResult retirar(Long cuentaId, String pin, BigDecimal monto) {
        if (!pinValido(pin)) {
            return fallo(RETIRO, cuentaId, monto, HttpStatus.UNAUTHORIZED, "PIN_INVALIDO");
        }
        CuentaSaldoFinal cuenta = saldoRepository.findById(cuentaId).orElse(null);
        if (cuenta == null) {
            return fallo(RETIRO, cuentaId, monto, HttpStatus.NOT_FOUND, "CUENTA_NO_EXISTE");
        }
        if (monto == null || monto.signum() <= 0) {
            return fallo(RETIRO, cuentaId, monto, HttpStatus.BAD_REQUEST, "MONTO_INVALIDO");
        }
        if (monto.compareTo(cuenta.getSaldo()) > 0) {
            return fallo(RETIRO, cuentaId, monto, HttpStatus.CONFLICT,
                    "SALDO_INSUFICIENTE");
        }

        BigDecimal saldoNuevo = cuenta.getSaldo().subtract(monto);
        cuenta.setSaldo(saldoNuevo);
        cuenta.setFechaActualizacion(LocalDate.now());
        saldoRepository.save(cuenta);

        Transaccion t = new Transaccion(
                transaccionRepository.findMaxId() + 1,
                LocalDate.now(),
                monto,
                "debito",
                "OK");
        Transaccion saved = transaccionRepository.save(t);

        AtmResult result = AtmResult.ok(RETIRO);
        result.getBody().put("cuenta_id", cuenta.getCuentaId());
        result.getBody().put("nombre", cuenta.getNombre());
        result.getBody().put("monto_retirado", monto);
        result.getBody().put("saldo_anterior", cuenta.getSaldo().add(monto));
        result.getBody().put("saldo_nuevo", saldoNuevo);
        result.getBody().put("transaccion_id", saved.getId());
        result.getBody().put("fecha", String.valueOf(LocalDateTime.now()));
        result.getBody().put("ticket", generarTicket(RETIRO));

        registrar(RETIRO, cuenta.getCuentaId(), monto, saldoNuevo, true, "Aprobada");
        return result;
    }

    /** Estado operativo del cajero. */
    public Map<String, Object> estado() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("estado", "OPERATIVO");
        map.put("cajero", cajeroId);
        map.put("version", cajeroVersion);
        map.put("fecha", String.valueOf(LocalDateTime.now()));
        return map;
    }

    /** Inventario de billetes disponible en el cajero. */
    public Map<String, Object> billetes() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("20000", 30);
        map.put("10000", 60);
        map.put("5000", 120);
        map.put("2000", 160);
        map.put("1000", 200);
        return map;
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private boolean pinValido(String pin) {
        return pin != null && pin.equals(pinDefault);
    }

    private AtmResult fallo(String tipo, Long cuentaId, BigDecimal monto,
                            HttpStatus status, String motivo) {
        registrar(tipo, cuentaId, monto, null, false, motivo);
        return AtmResult.fallo(status, motivo);
    }

    private void registrar(String tipo, Long cuentaId, BigDecimal monto,
                           BigDecimal saldoPosterior, boolean aprobado, String motivo) {
        AtmOperacion op = new AtmOperacion();
        op.setCuentaId(cuentaId);
        op.setTipo(tipo);
        op.setMonto(monto);
        op.setSaldoPosterior(saldoPosterior);
        op.setAprobado(aprobado);
        op.setMotivo(motivo);
        op.setFecha(LocalDateTime.now());
        operacionRepository.save(op);
    }

    private String generarTicket(String tipo) {
        return "TKT-" + cajeroId + "-" + (tipo.equals(RETIRO) ? "RT" : "SL")
                + "-" + System.currentTimeMillis();
    }
}