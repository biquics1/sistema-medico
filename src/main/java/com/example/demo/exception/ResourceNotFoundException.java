package com.example.demo.exception;

// Excepción para "recurso no encontrado" (ej. buscar por ID una cita, usuario o
// catálogo inexistente). El GlobalExceptionHandler la traduce a HTTP 404.
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) { super(message); }
}