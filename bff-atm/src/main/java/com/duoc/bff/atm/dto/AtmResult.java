package com.duoc.bff.atm.dto;

import org.springframework.http.HttpStatus;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Respuesta del cajero: estado HTTP + cuerpo JSON (operacion aprobada o
 * rechazada con su motivo).
 */
public class AtmResult {

    private final HttpStatus status;
    private final Map<String, Object> body;

    private AtmResult(HttpStatus status, Map<String, Object> body) {
        this.status = status;
        this.body = body;
    }

    public static AtmResult ok(String tipo) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("tipo", tipo);
        body.put("aprobado", true);
        return new AtmResult(HttpStatus.OK, body);
    }

    public static AtmResult fallo(HttpStatus status, String motivo) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("aprobado", false);
        body.put("motivo", motivo);
        return new AtmResult(status, body);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public Map<String, Object> getBody() {
        return body;
    }
}