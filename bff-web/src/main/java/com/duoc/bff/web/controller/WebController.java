package com.duoc.bff.web.controller;

import com.duoc.bff.web.dto.ApiResponse;
import com.duoc.bff.web.service.WebService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * API del BFF Web. Provee datos completos al navegador (dashboard,
 * transacciones, cuentas, estados de cuenta y estadisticas).
 */
@RestController
@RequestMapping("/api/web")
public class WebController {

    private final WebService webService;

    public WebController(WebService webService) {
        this.webService = webService;
    }

    private ApiResponse ok(String endpoint, Object datos) {
        return new ApiResponse("web", endpoint, datos);
    }

    @GetMapping("/dashboard")
    public ApiResponse dashboard() {
        return ok("/api/web/dashboard", webService.dashboard());
    }

    @GetMapping("/transacciones")
    public ApiResponse transacciones() {
        return ok("/api/web/transacciones", webService.todasLasTransacciones());
    }

    @GetMapping("/transacciones/{id}")
    public ResponseEntity<ApiResponse> transaccion(@PathVariable Long id) {
        return webService.transaccionPorId(id)
                .map(t -> ResponseEntity.ok(ok("/api/web/transacciones/" + id, t)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/cuentas")
    public ApiResponse cuentas() {
        return ok("/api/web/cuentas", webService.todasLasCuentas());
    }

    @GetMapping("/cuentas/{id}")
    public ResponseEntity<ApiResponse> cuenta(@PathVariable Long id) {
        Map<String, Object> detalle = webService.detalleCuenta(id);
        if (detalle == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ok("/api/web/cuentas/" + id, detalle));
    }

    @GetMapping("/estados-cuenta")
    public ApiResponse estadosDeCuenta() {
        return ok("/api/web/estados-cuenta", webService.estadosDeCuenta());
    }

    @GetMapping("/cuentas/{id}/movimientos")
    public ApiResponse movimientos(@PathVariable Long id) {
        return ok("/api/web/cuentas/" + id + "/movimientos", webService.movimientosDeCuenta(id));
    }

    @GetMapping("/estadisticas")
    public ApiResponse estadisticas() {
        return ok("/api/web/estadisticas", webService.estadisticas());
    }
}