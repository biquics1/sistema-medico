package com.example.demo.service;

import com.example.demo.dto.EventoAgendaDTO;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.ValidationException;
import com.example.demo.modelo.EventoAgenda;
import com.example.demo.modelo.Usuario;
import com.example.demo.repository.EventoAgendaRepository;
import com.example.demo.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventoAgendaService {

    private final EventoAgendaRepository repository;
    private final UsuarioRepository usuarioRepository;

    private static final Map<Short, String> TIPOS_EVENTO = Map.of(
            (short) 0, "Reunión",
            (short) 1, "Descanso",
            (short) 2, "Capacitación",
            (short) 3, "Personal",
            (short) 4, "Otro"
    );

    public List<EventoAgendaDTO.ResponseDTO> listarRango(Integer medicoId, LocalDateTime desde, LocalDateTime hasta) {
        return repository
                .findByMedico_IdAndFechaInicioLessThanEqualAndFechaFinGreaterThanEqualOrderByFechaInicioAsc(
                        medicoId, hasta, desde)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public EventoAgendaDTO.ResponseDTO crear(EventoAgendaDTO.CreateDTO dto, Integer medicoId) {
        validarFechas(dto);
        Usuario medico = usuarioRepository.findById(medicoId)
                .orElseThrow(() -> new ResourceNotFoundException("Médico no encontrado."));

        EventoAgenda e = new EventoAgenda();
        e.setMedico(medico);
        aplicarDto(e, dto);
        return toDto(repository.save(e));
    }

    @Transactional
    public EventoAgendaDTO.ResponseDTO actualizar(Integer id, EventoAgendaDTO.CreateDTO dto, Integer medicoId) {
        validarFechas(dto);
        EventoAgenda e = repository.findByIdAndMedico_Id(id, medicoId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado."));
        aplicarDto(e, dto);
        return toDto(repository.save(e));
    }

    @Transactional
    public void eliminar(Integer id, Integer medicoId) {
        EventoAgenda e = repository.findByIdAndMedico_Id(id, medicoId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado."));
        repository.delete(e);
    }

    private void aplicarDto(EventoAgenda e, EventoAgendaDTO.CreateDTO dto) {
        e.setTitulo(dto.getTitulo());
        e.setDescripcion(dto.getDescripcion());
        e.setTipoEvento(dto.getTipoEvento());
        e.setFechaInicio(dto.getFechaInicio());
        e.setFechaFin(dto.getFechaFin());
        e.setTodoElDia(dto.getTodoElDia() != null ? dto.getTodoElDia() : Boolean.FALSE);
    }

    // RN-CU14-01
    private void validarFechas(EventoAgendaDTO.CreateDTO dto) {
        if (dto.getFechaInicio() == null) {
            throw new ValidationException("La fecha de inicio es obligatoria.");
        }
        if (dto.getFechaFin() == null || !dto.getFechaFin().isAfter(dto.getFechaInicio())) {
            throw new ValidationException("La fecha de fin debe ser posterior a la fecha de inicio.");
        }
    }

    private EventoAgendaDTO.ResponseDTO toDto(EventoAgenda e) {
        EventoAgendaDTO.ResponseDTO dto = new EventoAgendaDTO.ResponseDTO();
        dto.setId(e.getId());
        dto.setTitulo(e.getTitulo());
        dto.setDescripcion(e.getDescripcion());
        dto.setTipoEvento(e.getTipoEvento());
        dto.setTipoEventoNombre(TIPOS_EVENTO.getOrDefault(e.getTipoEvento(), "Otro"));
        dto.setFechaInicio(e.getFechaInicio());
        dto.setFechaFin(e.getFechaFin());
        dto.setTodoElDia(e.getTodoElDia());
        dto.setMedicoId(e.getMedico().getId());
        return dto;
    }
}