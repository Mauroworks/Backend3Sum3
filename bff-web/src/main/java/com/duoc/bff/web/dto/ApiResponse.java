package com.duoc.bff.web.dto;

/**
 * Envoltorio JSON estandar de las respuestas del BFF Web.
 */
public class ApiResponse {

    private String bff;
    private String endpoint;
    private String mensaje;
    private Object datos;
    private long total;

    public ApiResponse() {
    }

    public ApiResponse(String bff, String endpoint, Object datos) {
        this.bff = bff;
        this.endpoint = endpoint;
        this.datos = datos;
        this.total = (datos instanceof java.util.List) ? ((java.util.List<?>) datos).size() : 1;
    }

    public ApiResponse(String bff, String endpoint, String mensaje, Object datos) {
        this(bff, endpoint, datos);
        this.mensaje = mensaje;
    }

    public String getBff() {
        return bff;
    }

    public void setBff(String bff) {
        this.bff = bff;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public Object getDatos() {
        return datos;
    }

    public void setDatos(Object datos) {
        this.datos = datos;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }
}