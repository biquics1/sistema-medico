package com.example.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.HashMap;
import java.util.Map;

// Manejador global de excepciones: centraliza cómo se convierten los distintos
// tipos de error en respuestas HTTP consistentes (siempre con la clave
// "mensaje" que el frontend espera leer en apiFetch de common.js).
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Errores de validación @Valid en los DTOs de entrada (ej. campos
    // obligatorios vacíos, formatos inválidos). Junta todos los mensajes de
    // campo en un solo "mensaje" legible, además de conservar el detalle por campo.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e ->
                errores.put(e.getField(), e.getDefaultMessage()));

        // CORREGIDO: el frontend (common.js -> apiFetch) solo lee data.mensaje /
        // data.message para mostrar el error al usuario. Antes esta respuesta
        // solo traía el mapa por campo (sin "mensaje"/"message"), así que
        // cualquier validación @Valid fallida (EventoAgendaDTO, TareaMedicoDTO,
        // MovimientoInventarioDTO, UsuarioCreateDTO, etc.) se mostraba como el
        // genérico "Ocurrió un error inesperado." en vez del mensaje real.
        String mensaje = String.join(" ", errores.values());

        Map<String, Object> body = new HashMap<>();
        body.put("mensaje", mensaje);
        body.put("errores", errores); // se conserva el detalle por campo por si se necesita
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("mensaje", ex.getMessage()));
    }

    // Reglas de negocio violadas (ej. duplicados, estados inválidos, stock insuficiente).
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, String>> handleBusinessValidation(ValidationException ex) {
        return ResponseEntity.badRequest().body(Map.of("mensaje", ex.getMessage()));
    }
}