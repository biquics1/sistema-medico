package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;

// >>> CU-13 (RN-CU12-01) - FB paso 7 / FA03: body para asignar una especialidad a una sede.
// Solo requiere los IDs de sede y especialidad; el estado se asigna activo por defecto.
public record BranchSpecialtyCreateDTO(
        // >>> VALIDACION FA03 / RN-CU12-01 (Bean Validation, se dispara con @Valid en el controller): sede obligatoria.
        @NotNull(message = "Debe seleccionar una sede.")
        Integer sucursalId,

        // >>> VALIDACION FA03 / RN-CU12-01: especialidad obligatoria.
        @NotNull(message = "Debe seleccionar una especialidad.")
        Integer especialidadId
) {}