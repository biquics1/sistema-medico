package com.example.demo.controller;

import com.example.demo.exception.ValidationException;
import com.example.demo.security.AuthUsuario;
import com.example.demo.service.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

// Reportería gráfica (Administrador de Sede y Administrador General).
// Administrador de Sede: siempre acotado a su sucursal (ignora sucursalId del request).
// Administrador General: sucursalId opcional (null = todas las sedes).
@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMINISTRADOR','ADMINISTRADOR GENERAL')")
public class ReporteController {

    private final ReporteService reporteService;

    @GetMapping("/catalogo")
    public List<Map<String, Object>> catalogo(@AuthenticationPrincipal AuthUsuario usuario) {
        return reporteService.catalogo(usuario.esAdminGeneral());
    }

    // GET /api/reportes/todos?desde=...&hasta=...&sucursalId=1
    @GetMapping("/todos")
    public List<Map<String, Object>> todos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @RequestParam(required = false) Integer sucursalId,
            @AuthenticationPrincipal AuthUsuario usuario) {
        return reporteService.todos(
                desdeODefecto(desde), hastaODefecto(hasta), sedeEfectiva(usuario, sucursalId), usuario.esAdminGeneral());
    }

    // GET /api/reportes/{clave}?desde=...&hasta=...&sucursalId=1
    @GetMapping("/{clave}")
    public Map<String, Object> generar(
            @PathVariable String clave,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @RequestParam(required = false) Integer sucursalId,
            @AuthenticationPrincipal AuthUsuario usuario) {
        return reporteService.generar(clave,
                desdeODefecto(desde), hastaODefecto(hasta), sedeEfectiva(usuario, sucursalId), usuario.esAdminGeneral());
    }

    private Integer sedeEfectiva(AuthUsuario usuario, Integer sucursalId) {
        Integer scope = usuario.sucursalScopeOrNull();
        if (usuario.esAdminSede() && scope == null) {
            throw new ValidationException("Su usuario no tiene una sede asignada.");
        }
        return scope != null ? scope : sucursalId;
    }

    private LocalDateTime desdeODefecto(LocalDateTime d) {
        return d != null ? d : LocalDate.now().withDayOfMonth(1).atStartOfDay();
    }

    private LocalDateTime hastaODefecto(LocalDateTime h) {
        return h != null ? h : LocalDate.now().atTime(LocalTime.MAX).withNano(0);
    }
}