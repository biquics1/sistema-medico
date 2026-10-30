package com.example.demo.dto;

// Fila de la tabla "Sucursal-Especialidad" (CU-12), con los nombres ya resueltos
// para mostrar directamente en el listado sin joins adicionales en el frontend.
// >>> FB PASO 3 (CU-13): columnas de la tabla = ID, Sede, Especialidad y Estado (las acciones son del front).
public record BranchSpecialtyResponseDTO(
        Integer id,
        Integer sucursalId,
        String sucursalNombre,
        Integer especialidadId,
        String especialidadNombre,
        Short estado
) {}