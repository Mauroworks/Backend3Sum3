package com.duoc.batch.procesadorf1.exception;

/**
 * Excepcion lanzada cuando un item no puede ser corregido ni procesado.
 * Permite que el Step continue (tolerancia a fallos) saltandose el registro
 * invalido gracias a la SkipPolicy personalizada.
 */
public class DataValidationException extends RuntimeException {

    public DataValidationException(String message) {
        super(message);
    }
}
