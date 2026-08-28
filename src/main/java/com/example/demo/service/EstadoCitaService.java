package com.example.demo.service;

import com.example.demo.dto.EstadoCitaDTO;
import com.example.demo.dto.PageResponseDTO;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.ValidationException;
import com.example.demo.modelo.EstadoCita;
import com.example.demo.repository.EstadoCitaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EstadoCitaService {
    private final AuditoriaContexto auditoriaContexto;

    private final EstadoCitaRepository repository;

    public PageResponseDTO<EstadoCitaDTO.ResponseDTO> listar(String filtro, Pageable pageable) {
        Page<EstadoCita> page = (filtro == null || filtro.isBlank())
                ? repository.findAll(pageable)
                : repository.findAll((root, query, cb) ->
                cb.like(cb.lower(root.get("nombre")), "%" + filtro.toLowerCase() + "%"), pageable);

        List<EstadoCitaDTO.ResponseDTO> contenido = page.getContent().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());

        return new PageResponseDTO<>(contenido, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    @Transactional
    public EstadoCitaDTO.ResponseDTO crear(EstadoCitaDTO.CreateDTO dto) {
        auditoriaContexto.aplicar();
        if (repository.existsByNombreIgnoreCaseAndEstado(dto.getNombre(), (short) 1)) {
            throw new ValidationException("Ya existe un registro con el nombre " + dto.getNombre() + " en este catálogo.");
        }
        EstadoCita entidad = new EstadoCita();
        entidad.setNombre(dto.getNombre());
        entidad.setDescripcion(dto.getDescripcion());
        entidad.setEstado(dto.getEstado() != null ? dto.getEstado() : 1);
        return toResponseDTO(repository.save(entidad));
    }

    @Transactional
    public EstadoCitaDTO.ResponseDTO actualizar(Integer id, EstadoCitaDTO.CreateDTO dto) {
        auditoriaContexto.aplicar();
        EstadoCita entidad = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Estado de cita no encontrado."));
        entidad.setNombre(dto.getNombre());
        entidad.setDescripcion(dto.getDescripcion());
        entidad.setEstado(dto.getEstado());
        return toResponseDTO(repository.save(entidad));
    }

    @Transactional
    public void eliminar(Integer id) {
        auditoriaContexto.aplicar();
        EstadoCita entidad = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Estado de cita no encontrado."));
        entidad.setEstado((short) 0);
        repository.save(entidad);
    }

    private EstadoCitaDTO.ResponseDTO toResponseDTO(EstadoCita e) {
        EstadoCitaDTO.ResponseDTO dto = new EstadoCitaDTO.ResponseDTO();
        dto.setId(e.getId());
        dto.setNombre(e.getNombre());
        dto.setDescripcion(e.getDescripcion());
        dto.setEstado(e.getEstado());
        return dto;
    }
}