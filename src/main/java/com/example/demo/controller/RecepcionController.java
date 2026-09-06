package com.example.demo.controller;

import com.example.demo.dto.CitaRecepcionDTO;
import com.example.demo.dto.RecepcionDTOs.*;
import com.example.demo.security.AuthUsuario;
import com.example.demo.service.RecepcionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Recepción y Verificación de Cita (CU-05): búsqueda de cita/paciente, registro
// de llegada y reasignación de médico (FA07).
@RestController
@RequestMapping("/api/recepcion")
@RequiredArgsConstructor
public class RecepcionController {

    private final RecepcionService recepcionService;

    // Paso 3 FB
    // GET /api/recepcion/citas/buscar?tipo=DPI&valor=1234567890123
    // GET /api/recepcion/citas/buscar?tipo=CITA&valor=1
    @GetMapping("/citas/buscar")
    public BusquedaResultadoDTO buscar(@RequestParam String tipo, @RequestParam String valor,
                                       @AuthenticationPrincipal AuthUsuario usuario) {
        return recepcionService.buscarCita(tipo, valor, usuario.sucursalScopeOrNull());
    }

    // Paso 6 FB
    @PostMapping("/citas/{idCita}/registrar-llegada")
    public RegistrarLlegadaResponseDTO registrarLlegada(@PathVariable Integer idCita,
                                                        @AuthenticationPrincipal AuthUsuario usuario) {
        return recepcionService.registrarLlegada(idCita, usuario.sucursalScopeOrNull());
    }

    // NUEVO — Cancelación de cita desde Recepción (a solicitud del paciente
    // o por indicación administrativa), mientras la cita no haya iniciado
    // su atención clínica. Ver RecepcionService.cancelarCita.
    @PostMapping("/citas/{idCita}/cancelar")
    public CancelarCitaResponseDTO cancelar(
            @PathVariable Integer idCita,
            @RequestBody(required = false) CancelarCitaRequestDTO request,
            @AuthenticationPrincipal AuthUsuario usuario) {
        String motivo = request != null ? request.getMotivo() : null;
        return recepcionService.cancelarCita(idCita, motivo, usuario.sucursalScopeOrNull());
    }

    // FA07 paso 2: médicos disponibles de la misma sede + especialidad
    @GetMapping("/medicos-disponibles")
    public List<MedicoDisponibleDTO> medicosDisponibles(
            @RequestParam Integer sucursalId,
            @RequestParam Integer especialidadId,
            @RequestParam Integer medicoActualId,
            @AuthenticationPrincipal AuthUsuario usuario) {
        return recepcionService.medicosDisponiblesParaReasignacion(
                sucursalId, especialidadId, medicoActualId, usuario.sucursalScopeOrNull());
    }

    // FA07 paso 3: confirmar reasignación.
    // idUsuarioReasigna ya no viaja en el body: sale del JWT.
    @PostMapping("/citas/{idCita}/reasignar-medico")
    public CitaRecepcionDTO reasignarMedico(
            @PathVariable Integer idCita,
            @RequestBody ReasignarMedicoRequestDTO request,
            @AuthenticationPrincipal AuthUsuario usuario) {
        return recepcionService.reasignarMedico(
                idCita, request.getIdMedicoNuevo(), request.getMotivo(), usuario.getId(), usuario.sucursalScopeOrNull());
    }
}
