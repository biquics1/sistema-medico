package com.example.demo.dto;

public record BranchSpecialtyResponseDTO(
        Integer id,
        Integer sucursalId,
        String sucursalNombre,
        Integer especialidadId,
        String especialidadNombre,
        Short estado
) {}