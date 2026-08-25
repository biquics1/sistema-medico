package com.example.demo.controller;

import com.example.demo.dto.CitaAgendaDTO;
import com.example.demo.security.AuthUsuario;
import com.example.demo.service.CitaService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/agenda-medica")
@RequiredArgsConstructor
public class AgendaMedicaController {

    private final CitaService citaService;

    // GET /api/agenda-medica/citas?desde=2026-08-01T00:00:00&hasta=2026-08-31T23:59:59
    @GetMapping("/citas")
    @PreAuthorize("hasRole('MÉDICO')")
    public List<CitaAgendaDTO> citas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @AuthenticationPrincipal AuthUsuario usuario) {
        return citaService.listarCitasAgenda(usuario.getId(), desde, hasta);
    }
}