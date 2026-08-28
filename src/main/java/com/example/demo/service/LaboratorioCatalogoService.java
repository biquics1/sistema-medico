package com.example.demo.service;

import com.example.demo.dto.LaboratorioCatalogoDTO;
import com.example.demo.dto.PageResponseDTO;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.ValidationException;
import com.example.demo.modelo.Laboratorio;
import com.example.demo.repository.LaboratorioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LaboratorioCatalogoService {
    private final AuditoriaContexto auditoriaContexto;

    private final LaboratorioRepository repository;

    public PageResponseDTO<LaboratorioCatalogoDTO.ResponseDTO> listar(String filtro, Pageable pageable) {
        Page<Laboratorio> page = (filtro == null || filtro.isBlank())
                ? repository.findAll(pageable)
                : repository.findAll((root, query, cb) ->
                cb.like(cb.lower(root.get("nombre")), "%" + filtro.toLowerCase() + "%"), pageable);

        List<LaboratorioCatalogoDTO.ResponseDTO> contenido = page.getContent().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());

        return new PageResponseDTO<>(contenido, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    @Transactional
    public LaboratorioCatalogoDTO.ResponseDTO crear(LaboratorioCatalogoDTO.CreateDTO dto) {
        auditoriaContexto.aplicar();
        if (repository.existsByNombreIgnoreCaseAndEstado(dto.getNombre(), (short) 1)) {
            throw new ValidationException("Ya existe un registro con el nombre " + dto.getNombre() + " en este catálogo.");
        }
        Laboratorio entidad = new Laboratorio();
        entidad.setNombre(dto.getNombre());
        entidad.setDescripcion(dto.getDescripcion());
        entidad.setEstado(dto.getEstado() != null ? dto.getEstado() : 1);
        return toResponseDTO(repository.save(entidad));
    }

    @Transactional
    public LaboratorioCatalogoDTO.ResponseDTO actualizar(Integer id, LaboratorioCatalogoDTO.CreateDTO dto) {
        auditoriaContexto.aplicar();
        Laboratorio entidad = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Laboratorio no encontrado."));

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
        Laboratorio entidad = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Laboratorio no encontrado."));
        entidad.setEstado((short) 0);
        repository.save(entidad);
    }

    private LaboratorioCatalogoDTO.ResponseDTO toResponseDTO(Laboratorio l) {
        LaboratorioCatalogoDTO.ResponseDTO dto = new LaboratorioCatalogoDTO.ResponseDTO();
        dto.setId(l.getId());
        dto.setNombre(l.getNombre());
        dto.setDescripcion(l.getDescripcion());
        dto.setEstado(l.getEstado());
        return dto;
    }
}