package com.duoc.bff.atm.controller;

import com.duoc.bff.atm.dto.AtmRequest;
import com.duoc.bff.atm.dto.AtmResult;
import com.duoc.bff.atm.model.AtmOperacion;
import com.duoc.bff.atm.repository.AtmOperacionRepository;
import com.duoc.bff.atm.service.AtmService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API del BFF Cajeros Automaticos: operaciones criticas (saldo y retiro) con
 * validacion de PIN, mas estado del cajero, billetes y auditoria.
 */
@RestController
@RequestMapping("/api/atm")
public class AtmController {

    private final AtmService atmService;
    private final AtmOperacionRepository operacionRepository;

    public AtmController(AtmService atmService,
                         AtmOperacionRepository operacionRepository) {
        this.atmService = atmService;
        this.operacionRepository = operacionRepository;
    }

    @GetMapping("/estado")
    public Object estado() {
        return atmService.estado();
    }

    @GetMapping("/billetes")
    public Object billetes() {
        return atmService.billetes();
    }

    @PostMapping("/saldo")
    public ResponseEntity<Object> saldo(@RequestBody AtmRequest req) {
        AtmResult result = atmService.consultarSaldo(req.getCuentaId(), req.getPin());
        return ResponseEntity.status(result.getStatus()).body(result.getBody());
    }

    @PostMapping("/retiro")
    public ResponseEntity<Object> retiro(@RequestBody AtmRequest req) {
        AtmResult result = atmService.retirar(req.getCuentaId(), req.getPin(), req.getMonto());
        return ResponseEntity.status(result.getStatus()).body(result.getBody());
    }

    @GetMapping("/operaciones")
    public List<AtmOperacion> operaciones(
            @RequestParam(required = false) Long cuentaId) {
        if (cuentaId == null) {
            return operacionRepository.findAllByOrderByFechaDesc();
        }
        return operacionRepository.findAllByCuentaIdOrderByFechaDesc(cuentaId);
    }
}