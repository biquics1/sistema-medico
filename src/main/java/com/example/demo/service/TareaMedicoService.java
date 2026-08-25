package com.example.demo.service;

import com.example.demo.dto.TareaMedicoDTO;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.ValidationException;
import com.example.demo.modelo.TareaMedico;
import com.example.demo.modelo.Usuario;
import com.example.demo.repository.TareaMedicoRepository;
import com.example.demo.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TareaMedicoService {

    private final TareaMedicoRepository repository;
    private final UsuarioRepository usuarioRepository;

    private static final Map<Short, String> PRIORIDADES = Map.of(
            (short) 0, "Baja",
            (short) 1, "Normal",
            (short) 2, "Alta"
    );

    // filtro: "pendientes" | "completadas" | null/"todas"
    public List<TareaMedicoDTO.ResponseDTO> listar(Integer medicoId, String filtro) {
        List<TareaMedico> tareas;
        if ("pendientes".equalsIgnoreCase(filtro)) {
            tareas = repository.findByMedico_IdAndCompletadaOrderByFechaLimiteAsc(medicoId, false);
        } else if ("completadas".equalsIgnoreCase(filtro)) {
            tareas = repository.findByMedico_IdAndCompletadaOrderByFechaLimiteAsc(medicoId, true);
        } else {
            tareas = repository.findByMedico_IdOrderByFechaLimiteAsc(medicoId);
        }
        return tareas.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public TareaMedicoDTO.ResponseDTO crear(TareaMedicoDTO.CreateDTO dto, Integer medicoId) {
        validarFechaLimite(dto.getFechaLimite());
        Usuario medico = usuarioRepository.findById(medicoId)
                .orElseThrow(() -> new ResourceNotFoundException("Médico no encontrado."));

        TareaMedico t = new TareaMedico();
        t.setMedico(medico);
        t.setCompletada(false);
        aplicarDto(t, dto);
        return toDto(repository.save(t));
    }

    @Transactional
    public TareaMedicoDTO.ResponseDTO actualizar(Integer id, TareaMedicoDTO.CreateDTO dto, Integer medicoId) {
        TareaMedico t = repository.findByIdAndMedico_Id(id, medicoId)
                .orElseThrow(() -> new ResourceNotFoundException("Tarea no encontrada."));

        // CORREGIDO: solo exigir "fecha futura" cuando el médico realmente está
        // fijando/cambiando la fecha límite. Antes se validaba en cada
        // actualización sin importar qué campo cambiara, así que una tarea con
        // fecha límite ya vencida quedaba imposible de editar (título,
        // descripción, prioridad) sin antes mover la fecha al futuro.
        boolean fechaLimiteCambio = !java.util.Objects.equals(dto.getFechaLimite(), t.getFechaLimite());
        if (fechaLimiteCambio) {
            validarFechaLimite(dto.getFechaLimite());
        }

        aplicarDto(t, dto);
        return toDto(repository.save(t));
    }

    @Transactional
    public TareaMedicoDTO.ResponseDTO cambiarCompletada(Integer id, boolean completada, Integer medicoId) {
        TareaMedico t = repository.findByIdAndMedico_Id(id, medicoId)
                .orElseThrow(() -> new ResourceNotFoundException("Tarea no encontrada."));
        t.setCompletada(completada);
        return toDto(repository.save(t));
    }

    @Transactional
    public void eliminar(Integer id, Integer medicoId) {
        TareaMedico t = repository.findByIdAndMedico_Id(id, medicoId)
                .orElseThrow(() -> new ResourceNotFoundException("Tarea no encontrada."));
        repository.delete(t);
    }

    private void aplicarDto(TareaMedico t, TareaMedicoDTO.CreateDTO dto) {
        t.setTitulo(dto.getTitulo());
        t.setDescripcion(dto.getDescripcion());
        t.setPrioridad(dto.getPrioridad());
        t.setFechaLimite(dto.getFechaLimite());
    }

    private void validarFechaLimite(LocalDate fechaLimite) {
        if (fechaLimite != null && fechaLimite.isBefore(LocalDate.now())) {
            throw new ValidationException("La fecha límite debe ser una fecha futura.");
        }
    }

    private TareaMedicoDTO.ResponseDTO toDto(TareaMedico t) {
        TareaMedicoDTO.ResponseDTO dto = new TareaMedicoDTO.ResponseDTO();
        dto.setId(t.getId());
        dto.setTitulo(t.getTitulo());
        dto.setDescripcion(t.getDescripcion());
        dto.setPrioridad(t.getPrioridad());
        dto.setPrioridadNombre(PRIORIDADES.getOrDefault(t.getPrioridad(), "Normal"));
        dto.setFechaLimite(t.getFechaLimite());
        dto.setCompletada(t.getCompletada());
        dto.setMedicoId(t.getMedico().getId());
        return dto;
    }
}