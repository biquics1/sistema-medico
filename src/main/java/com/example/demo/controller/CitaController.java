package com.example.demo.controller;

import com.example.demo.dto.*;
import com.example.demo.security.AuthUsuario;
import com.example.demo.service.CitaService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

// Wizard de agendamiento de citas (CU-03): expone sucursales, especialidades por
// sede, médicos disponibles, horarios libres y la creación/consulta de la cita.
@RestController
@RequiredArgsConstructor
public class CitaController {

    private final CitaService citaService;

    @GetMapping("/api/branches")
    public Object sucursales() {
        return citaService.listarSucursalesActivas();
    }

    @GetMapping("/api/branches/{sucursalId}/specialties")
    public Object especialidades(@PathVariable Integer sucursalId) {
        return citaService.listarEspecialidadesPorSucursal(sucursalId);
    }

    @GetMapping("/api/branches/{sucursalId}/specialties/{especialidadId}/doctors")
    public List<DoctorDisponibleDTO> medicos(@PathVariable Integer sucursalId,
                                             @PathVariable Integer especialidadId) {
        return citaService.listarMedicos(sucursalId, especialidadId);
    }

    @GetMapping("/api/doctors/{medicoId}/available-slots")
    public Map<String, Object> horarios(@PathVariable Integer medicoId,
                                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Map.of("horarios", citaService.listarHorariosDisponibles(medicoId, date));
    }

    @PostMapping("/api/appointments")
    public CitaResponseDTO crear(@RequestBody CitaCreateDTO dto) {
        return citaService.crearCita(dto);
    }

    @GetMapping("/api/appointments/{id}")
    public CitaResponseDTO obtener(@PathVariable Integer id) {
        return citaService.obtenerCita(id);
    }

    // NUEVO — paso "método de pago" del wizard (paso 6, tras confirmar la cita):
    // el paciente elige "CAJA" o "LINEA". Ver CitaService.elegirMetodoPago.
    @PostMapping("/api/appointments/{id}/metodo-pago")
    public CitaResponseDTO elegirMetodoPago(@PathVariable Integer id,
                                            @RequestBody MetodoPagoDTO dto,
                                            @AuthenticationPrincipal AuthUsuario usuario) {
        return citaService.elegirMetodoPago(id, usuario.getId(), dto.getMetodoPago());
    }

    // NUEVO — el frontend llama esto cuando el contador de pago en línea
    // llega a 0, para cancelar la cita de inmediato sin esperar al job.
    @PostMapping("/api/appointments/{id}/cancelar-expirada")
    public CitaResponseDTO cancelarExpirada(@PathVariable Integer id,
                                            @AuthenticationPrincipal AuthUsuario usuario) {
        return citaService.cancelarPorExpiracionInmediata(id, usuario.getId());
    }
}