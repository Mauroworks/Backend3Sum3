package com.duoc.bff.mobile.controller;

import com.duoc.bff.mobile.service.MobileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * API del BFF Movil: endpoints livianos (sin envoltorio pesado) pensados para
 * apps moviles Android/iOS con conexiones lentas.
 */
@RestController
@RequestMapping("/api/mobile")
public class MobileController {

    private final MobileService mobileService;

    public MobileController(MobileService mobileService) {
        this.mobileService = mobileService;
    }

    @GetMapping("/resumen")
    public Map<String, Object> resumen() {
        return mobileService.resumen();
    }

    @GetMapping("/saldos")
    public Object saldos() {
        return mobileService.saldos();
    }

    @GetMapping("/transacciones")
    public Object transacciones(@RequestParam(defaultValue = "30") int limit) {
        int l = Math.min(Math.max(limit, 1), 200);
        return mobileService.transacciones(l);
    }

    @GetMapping("/cuenta/{id}/resumen")
    public ResponseEntity<Map<String, Object>> resumenCuenta(@PathVariable Long id) {
        Map<String, Object> resumen = mobileService.resumenCuenta(id);
        if (resumen == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(resumen);
    }

    @GetMapping("/cuenta/{id}/movimientos")
    public Object movimientos(@PathVariable Long id,
                              @RequestParam(defaultValue = "10") int limit) {
        int l = Math.min(Math.max(limit, 1), 100);
        return mobileService.movimientos(id, l);
    }
}