package com.example.demo.controller;

import com.example.demo.dto.EnfermeriaDTOs.*;
import com.example.demo.security.AuthUsuario;
import com.example.demo.service.EnfermeriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

// Módulo de Enfermería (CU-07 Toma de Signos Vitales): cola de pacientes,
// llamado por altavoz (TTS) y registro de signos vitales con alertas clínicas.
@RestController
@RequestMapping("/api/enfermeria")
@RequiredArgsConstructor
public class EnfermeriaController {

    private final EnfermeriaService enfermeriaService;

    // Paso 1 FB: GET /api/enfermeria/cola
    @GetMapping("/cola")
    public ColaEnfermeriaDTO cola(@AuthenticationPrincipal AuthUsuario usuario) {
        return enfermeriaService.obtenerCola(usuario.sucursalScopeOrNull());
    }

    // Paso 1 FB: POST /api/enfermeria/citas/{idCita}/llamar
    @PostMapping("/citas/{idCita}/llamar")
    public LlamarPacienteResponseDTO llamar(@PathVariable Integer idCita,
                                            @AuthenticationPrincipal AuthUsuario usuario) {
        return enfermeriaService.llamarPaciente(idCita, usuario.sucursalScopeOrNull());
    }

    // Paso 4-11 FB: POST /api/enfermeria/citas/{idCita}/signos-vitales
    // idEnfermero ya no viaja en el body: sale del JWT (rol Enfermero/Administrador).
    @PostMapping("/citas/{idCita}/signos-vitales")
    public SignosVitalesResponseDTO registrarSignos(@PathVariable Integer idCita,
                                                    @RequestBody RegistrarSignosVitalesRequestDTO request,
                                                    @AuthenticationPrincipal AuthUsuario usuario) {
        return enfermeriaService.registrarSignosVitales(idCita, request, usuario.getId(), usuario.sucursalScopeOrNull());
    }

    // "Llamar de nuevo": reintento de anuncio sin cambiar de estado (máximo 3 llamados en total)
    @PostMapping("/citas/{idCita}/llamar-de-nuevo")
    public LlamarPacienteResponseDTO llamarDeNuevo(@PathVariable Integer idCita,
                                                   @AuthenticationPrincipal AuthUsuario usuario) {
        return enfermeriaService.llamarDeNuevo(idCita, usuario.sucursalScopeOrNull());
    }

    // "No Asistió": el paciente no llegó a signos vitales pese a los llamados -> cancela la cita
    @PostMapping("/citas/{idCita}/no-asistio")
    public AccionCitaEnfermeriaResponseDTO noAsistio(@PathVariable Integer idCita,
                                                     @AuthenticationPrincipal AuthUsuario usuario) {
        return enfermeriaService.marcarNoAsistio(idCita, usuario.sucursalScopeOrNull());
    }
}