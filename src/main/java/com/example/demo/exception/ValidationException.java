package com.example.demo.exception;

// Excepción para violaciones de reglas de negocio (ej. duplicados, datos
// inconsistentes, stock insuficiente). El GlobalExceptionHandler la traduce a HTTP 400.
public class ValidationException extends RuntimeException {
    public ValidationException(String message) { super(message); }
}