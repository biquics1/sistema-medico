package com.example.demo.service;

import com.example.demo.dto.EspecialidadDTO;
import com.example.demo.dto.PageResponseDTO;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.ValidationException;
import com.example.demo.modelo.Especialidad;
import com.example.demo.repository.EspecialidadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EspecialidadService {
    private final AuditoriaContexto auditoriaContexto;

    private final EspecialidadRepository repository;

    public PageResponseDTO<EspecialidadDTO.ResponseDTO> listar(String filtro, Pageable pageable) {
        Page<Especialidad> page = (filtro == null || filtro.isBlank())
                ? repository.findAll(pageable)
                : repository.findAll((root, query, cb) ->
                cb.like(cb.lower(root.get("nombre")), "%" + filtro.toLowerCase() + "%"), pageable);

        List<EspecialidadDTO.ResponseDTO> contenido = page.getContent().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());

        return new PageResponseDTO<>(contenido, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    @Transactional
    public EspecialidadDTO.ResponseDTO crear(EspecialidadDTO.CreateDTO dto) {
        auditoriaContexto.aplicar();
        if (repository.existsByNombreIgnoreCaseAndEstado(dto.getNombre(), (short) 1)) {
            throw new ValidationException("Ya existe un registro con el nombre " + dto.getNombre() + " en este catálogo.");
        }
        Especialidad entidad = new Especialidad();
        entidad.setNombre(dto.getNombre());
        entidad.setDescripcion(dto.getDescripcion());
        entidad.setEstado(dto.getEstado() != null ? dto.getEstado() : 1);
        return toResponseDTO(repository.save(entidad));
    }

    @Transactional
    public EspecialidadDTO.ResponseDTO actualizar(Integer id, EspecialidadDTO.CreateDTO dto) {
        auditoriaContexto.aplicar();
        Especialidad entidad = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Especialidad no encontrada."));

        if (dto.getEstado() != null && dto.getEstado() == 1
                && repository.existsByNombreIgnoreCaseAndEstadoAndIdNot(dto.getNombre(), (short) 1, id)) {
            throw new ValidationException("Ya existe un registro con el nombre " + dto.getNombre() + " en este catálogo.");
        }

        entidad.setNombre(dto.getNombre());
        entidad.setDescripcion(dto.getDescripcion());
        entidad.setEstado(dto.getEstado());
        return toResponseDTO(repository.save(entidad));
    }

    @Transactional
    public void eliminar(Integer id) {
        auditoriaContexto.aplicar();
        Especialidad entidad = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Especialidad no encontrada."));
        entidad.setEstado((short) 0);
        repository.save(entidad);
    }

    private EspecialidadDTO.ResponseDTO toResponseDTO(Especialidad e) {
        EspecialidadDTO.ResponseDTO dto = new EspecialidadDTO.ResponseDTO();
        dto.setId(e.getId());
        dto.setNombre(e.getNombre());
        dto.setDescripcion(e.getDescripcion());
        dto.setEstado(e.getEstado());
        return dto;
    }
}