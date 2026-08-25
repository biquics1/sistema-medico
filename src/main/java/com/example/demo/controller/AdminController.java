package com.example.demo.controller;

import com.example.demo.dto.AdminMedicoDTO;
import com.example.demo.dto.CitaAgendaDTO;
import com.example.demo.security.AuthUsuario;
import com.example.demo.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * "Control de Citas por Sede": el Administrador (de sede o general) puede
 * ver a los médicos y su calendario de citas para monitorear cómo se
 * mueve la operación.
 *
 * La autorización de ruta (ROLE_ADMINISTRADOR / ROLE_ADMINISTRADOR GENERAL)
 * ya está resuelta en SecurityConfig ("/api/admin/**"). Aquí solo se aplica
 * el filtrado por sede vía AuthUsuario.sucursalScopeOrNull():
 *   - Administrador de Sede -> null solo si NO tiene sede (no debería pasar)
 *   - Administrador General -> siempre null -> sin restricción
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // GET /api/admin/medicos
    @GetMapping("/medicos")
    public List<AdminMedicoDTO> medicos(@AuthenticationPrincipal AuthUsuario admin) {
        return adminService.listarMedicos(admin.sucursalScopeOrNull());
    }

    // GET /api/admin/medicos/{id}/citas?desde=2026-08-01T00:00:00&hasta=2026-08-31T23:59:59
    @GetMapping("/medicos/{id}/citas")
    public List<CitaAgendaDTO> citasDeMedico(
            @PathVariable("id") Integer medicoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @AuthenticationPrincipal AuthUsuario admin) {
        return adminService.agendaDeMedico(medicoId, desde, hasta, admin.sucursalScopeOrNull());
    }
}
