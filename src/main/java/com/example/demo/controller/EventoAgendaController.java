package com.example.demo.controller;

import com.example.demo.dto.EventoAgendaDTO;
import com.example.demo.security.AuthUsuario;
import com.example.demo.service.EventoAgendaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/agenda-medica/eventos")
@RequiredArgsConstructor
public class EventoAgendaController {

    private final EventoAgendaService service;

    // GET /api/agenda-medica/eventos?desde=2026-08-01T00:00:00&hasta=2026-08-31T23:59:59
    @GetMapping
    @PreAuthorize("hasRole('MÉDICO')")
    public List<EventoAgendaDTO.ResponseDTO> listar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @AuthenticationPrincipal AuthUsuario usuario) {
        return service.listarRango(usuario.getId(), desde, hasta);
    }

    @PostMapping
    @PreAuthorize("hasRole('MÉDICO')")
    public ResponseEntity<EventoAgendaDTO.ResponseDTO> crear(@Valid @RequestBody EventoAgendaDTO.CreateDTO dto,
                                                             @AuthenticationPrincipal AuthUsuario usuario) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(dto, usuario.getId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MÉDICO')")
    public ResponseEntity<EventoAgendaDTO.ResponseDTO> actualizar(@PathVariable Integer id,
                                                                  @Valid @RequestBody EventoAgendaDTO.CreateDTO dto,
                                                                  @AuthenticationPrincipal AuthUsuario usuario) {
        return ResponseEntity.ok(service.actualizar(id, dto, usuario.getId()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MÉDICO')")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id, @AuthenticationPrincipal AuthUsuario usuario) {
        service.eliminar(id, usuario.getId());
        return ResponseEntity.noContent().build();
    }
}