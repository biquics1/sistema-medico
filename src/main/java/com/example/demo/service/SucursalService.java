package com.example.demo.service;

import com.example.demo.dto.PageResponseDTO;
import com.example.demo.dto.SucursalDTO;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.ValidationException;
import com.example.demo.modelo.Sucursal;
import com.example.demo.repository.SucursalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SucursalService {
    private final AuditoriaContexto auditoriaContexto;

    private final SucursalRepository repository;

    public PageResponseDTO<SucursalDTO.ResponseDTO> listar(String filtro, Pageable pageable) {
        Page<Sucursal> page = (filtro == null || filtro.isBlank())
                ? repository.findAll(pageable)
                : repository.findAll((root, query, cb) ->
                cb.like(cb.lower(root.get("nombre")), "%" + filtro.toLowerCase() + "%"), pageable);

        List<SucursalDTO.ResponseDTO> contenido = page.getContent().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());

        return new PageResponseDTO<>(contenido, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    @Transactional
    public SucursalDTO.ResponseDTO crear(SucursalDTO.CreateDTO dto) {
        auditoriaContexto.aplicar();
        if (repository.existsByNombreIgnoreCaseAndEstado(dto.getNombre(), (short) 1)) {
            throw new ValidationException("Ya existe un registro con el nombre " + dto.getNombre() + " en este catálogo.");
        }
        Sucursal entidad = new Sucursal();
        entidad.setNombre(dto.getNombre());
        entidad.setTelefono(dto.getTelefono());
        entidad.setDireccion(dto.getDireccion());
        entidad.setDescripcion(dto.getDescripcion());
        entidad.setEstado(dto.getEstado() != null ? dto.getEstado() : 1);
        return toResponseDTO(repository.save(entidad));
    }

    @Transactional
    public SucursalDTO.ResponseDTO actualizar(Integer id, SucursalDTO.CreateDTO dto) {
        auditoriaContexto.aplicar();
        Sucursal entidad = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada."));

        if (dto.getEstado() != null && dto.getEstado() == 1
                && repository.existsByNombreIgnoreCaseAndEstadoAndIdNot(dto.getNombre(), (short) 1, id)) {
            throw new ValidationException("Ya existe un registro con el nombre " + dto.getNombre() + " en este catálogo.");
        }

        entidad.setNombre(dto.getNombre());
        entidad.setTelefono(dto.getTelefono());
        entidad.setDireccion(dto.getDireccion());
        entidad.setDescripcion(dto.getDescripcion());
        entidad.setEstado(dto.getEstado());
        return toResponseDTO(repository.save(entidad));
    }

    @Transactional
    public void eliminar(Integer id) {
        auditoriaContexto.aplicar();
        Sucursal entidad = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada."));
        entidad.setEstado((short) 0);
        repository.save(entidad);
    }

    private SucursalDTO.ResponseDTO toResponseDTO(Sucursal s) {
        SucursalDTO.ResponseDTO dto = new SucursalDTO.ResponseDTO();
        dto.setId(s.getId());
        dto.setNombre(s.getNombre());
        dto.setTelefono(s.getTelefono());
        dto.setDireccion(s.getDireccion());
        dto.setDescripcion(s.getDescripcion());
        dto.setEstado(s.getEstado());
        return dto;
    }
}