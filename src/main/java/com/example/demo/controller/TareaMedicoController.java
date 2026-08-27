package com.example.demo.controller;

import com.example.demo.dto.TareaMedicoDTO;
import com.example.demo.security.AuthUsuario;
import com.example.demo.service.TareaMedicoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Panel lateral de tareas (TaskPanel) del médico en su Agenda Médica (CU-14):
// CRUD de tareas/recordatorios con prioridad y estado completada/pendiente.
@RestController
@RequestMapping("/api/agenda-medica/tareas")
@RequiredArgsConstructor
public class TareaMedicoController {

    private final TareaMedicoService service;

    // GET /api/agenda-medica/tareas?filtro=pendientes|completadas|todas
    @GetMapping
    @PreAuthorize("hasRole('MÉDICO')")
    public List<TareaMedicoDTO.ResponseDTO> listar(@RequestParam(required = false) String filtro,
                                                   @AuthenticationPrincipal AuthUsuario usuario) {
        return service.listar(usuario.getId(), filtro);
    }

    @PostMapping
    @PreAuthorize("hasRole('MÉDICO')")
    public ResponseEntity<TareaMedicoDTO.ResponseDTO> crear(@Valid @RequestBody TareaMedicoDTO.CreateDTO dto,
                                                            @AuthenticationPrincipal AuthUsuario usuario) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(dto, usuario.getId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MÉDICO')")
    public ResponseEntity<TareaMedicoDTO.ResponseDTO> actualizar(@PathVariable Integer id,
                                                                 @Valid @RequestBody TareaMedicoDTO.CreateDTO dto,
                                                                 @AuthenticationPrincipal AuthUsuario usuario) {
        return ResponseEntity.ok(service.actualizar(id, dto, usuario.getId()));
    }

    // Completar / descompletar (RN-CU14-02: isCompleted)
    @PatchMapping("/{id}/completar")
    @PreAuthorize("hasRole('MÉDICO')")
    public ResponseEntity<TareaMedicoDTO.ResponseDTO> completar(@PathVariable Integer id,
                                                                @RequestParam boolean completada,
                                                                @AuthenticationPrincipal AuthUsuario usuario) {
        return ResponseEntity.ok(service.cambiarCompletada(id, completada, usuario.getId()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MÉDICO')")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id, @AuthenticationPrincipal AuthUsuario usuario) {
        service.eliminar(id, usuario.getId());
        return ResponseEntity.noContent().build();
    }
}