package com.example.demo.controller;

import com.example.demo.dto.PacienteDTOs.MiCitaDetalleDTO;
import com.example.demo.dto.PacienteDTOs.MiCitaResumenDTO;
import com.example.demo.dto.PacienteDTOs.ReagendarCitaRequestDTO;
import com.example.demo.security.AuthUsuario;
import com.example.demo.service.PacienteAgendaService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Portal del paciente, sección "Mis Citas": listado resumido y detalle completo
// de las citas propias (incluye consulta, orden de laboratorio y recetas).
@RestController
@RequestMapping("/api/mis-citas")
@RequiredArgsConstructor
public class PacienteAgendaController {

    private final PacienteAgendaService service;

    @GetMapping
    @PreAuthorize("hasRole('PACIENTE')")
    public List<MiCitaResumenDTO> listar(@AuthenticationPrincipal AuthUsuario usuario) {
        return service.listarMisCitas(usuario.getId());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('PACIENTE')")
    public MiCitaDetalleDTO detalle(@PathVariable Integer id, @AuthenticationPrincipal AuthUsuario usuario) {
        return service.obtenerDetalle(id, usuario.getId());
    }

    // NUEVO -- Reagendar cita propia: solo cambia fecha/hora, el medico asignado no cambia.
    @PutMapping("/{id}/reagendar")
    @PreAuthorize("hasRole('PACIENTE')")
    public MiCitaResumenDTO reagendar(@PathVariable Integer id,
                                      @RequestBody ReagendarCitaRequestDTO request,
                                      @AuthenticationPrincipal AuthUsuario usuario) {
        return service.reagendarCita(id, usuario.getId(), request.getFechaHora());
    }
}