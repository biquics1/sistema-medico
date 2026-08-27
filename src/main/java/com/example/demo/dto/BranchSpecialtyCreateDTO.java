package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;

// Body para asignar una especialidad a una sede (CU-12 / CU-15).
// Solo requiere los IDs de sede y especialidad; el estado se asigna activo por defecto.
public record BranchSpecialtyCreateDTO(
        @NotNull(message = "Debe seleccionar una sede.")
        Integer sucursalId,

        @NotNull(message = "Debe seleccionar una especialidad.")
        Integer especialidadId
) {}