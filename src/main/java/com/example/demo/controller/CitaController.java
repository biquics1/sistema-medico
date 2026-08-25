package com.example.demo.controller;

import com.example.demo.dto.*;
import com.example.demo.service.CitaService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

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
}