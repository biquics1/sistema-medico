package com.example.demo.service;

import com.example.demo.dto.PageResponseDTO;
import com.example.demo.dto.RolDTO;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.ValidationException;
import com.example.demo.modelo.Rol;
import com.example.demo.repository.RolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RolService {
    private final AuditoriaContexto auditoriaContexto;

    private final RolRepository repository;

    public PageResponseDTO<RolDTO.ResponseDTO> listar(String filtro, Pageable pageable) {
        Page<Rol> page = (filtro == null || filtro.isBlank())
                ? repository.findAll(pageable)
                : repository.findAll((root, query, cb) ->
                cb.like(cb.lower(root.get("nombre")), "%" + filtro.toLowerCase() + "%"), pageable);

        List<RolDTO.ResponseDTO> contenido = page.getContent().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());

        return new PageResponseDTO<>(contenido, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    @Transactional
    public RolDTO.ResponseDTO crear(RolDTO.CreateDTO dto) {
        auditoriaContexto.aplicar();
        if (repository.existsByNombreIgnoreCaseAndEstado(dto.getNombre(), (short) 1)) {
            throw new ValidationException("Ya existe un registro con el nombre " + dto.getNombre() + " en este catálogo.");
        }
        Rol entidad = new Rol();
        entidad.setNombre(dto.getNombre());
        entidad.setDescripcion(dto.getDescripcion());
        entidad.setEstado(dto.getEstado() != null ? dto.getEstado() : 1);
        return toResponseDTO(repository.save(entidad));
    }

    @Transactional
    public RolDTO.ResponseDTO actualizar(Integer id, RolDTO.CreateDTO dto) {
        auditoriaContexto.aplicar();
        Rol entidad = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado."));

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
        Rol entidad = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado."));
        entidad.setEstado((short) 0);
        repository.save(entidad);
    }

    private RolDTO.ResponseDTO toResponseDTO(Rol r) {
        RolDTO.ResponseDTO dto = new RolDTO.ResponseDTO();
        dto.setId(r.getId());
        dto.setNombre(r.getNombre());
        dto.setDescripcion(r.getDescripcion());
        dto.setEstado(r.getEstado());
        return dto;
    }
}