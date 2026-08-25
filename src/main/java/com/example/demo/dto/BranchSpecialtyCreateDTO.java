package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;

public record BranchSpecialtyCreateDTO(
        @NotNull(message = "Debe seleccionar una sede.")
        Integer sucursalId,

        @NotNull(message = "Debe seleccionar una especialidad.")
        Integer especialidadId
) {}