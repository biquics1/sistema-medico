package com.example.demo.service;

import com.example.demo.dto.ExamenLaboratorioDTO;
import com.example.demo.dto.PageResponseDTO;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.ValidationException;
import com.example.demo.modelo.ExamenLaboratorio;
import com.example.demo.modelo.Laboratorio;
import com.example.demo.repository.ExamenLaboratorioRepository;
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
public class ExamenLaboratorioService {

    private final ExamenLaboratorioRepository repository;
    private final LaboratorioRepository laboratorioRepository;

    public PageResponseDTO<ExamenLaboratorioDTO.ResponseDTO> listar(String filtro, Pageable pageable) {
        Page<ExamenLaboratorio> page = (filtro == null || filtro.isBlank())
                ? repository.findAll(pageable)
                : repository.findAll((root, query, cb) ->
                cb.like(cb.lower(root.get("nombre")), "%" + filtro.toLowerCase() + "%"), pageable);

        List<ExamenLaboratorioDTO.ResponseDTO> contenido = page.getContent().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());

        return new PageResponseDTO<>(contenido, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    @Transactional
    public ExamenLaboratorioDTO.ResponseDTO crear(ExamenLaboratorioDTO.CreateDTO dto) {
        validar(dto);
        if (repository.existsByNombreIgnoreCaseAndEstado(dto.getNombre(), (short) 1)) {
            throw new ValidationException("Ya existe un registro con el nombre " + dto.getNombre() + " en este catálogo.");
        }
        Laboratorio laboratorio = laboratorioRepository.findById(dto.getLaboratorioId())
                .orElseThrow(() -> new ValidationException("Debe seleccionar un laboratorio."));

        ExamenLaboratorio entidad = new ExamenLaboratorio();
        entidad.setCodigo(dto.getCodigo());
        entidad.setNombre(dto.getNombre());
        entidad.setPrecio(dto.getPrecio());
        entidad.setRangoReferencia(dto.getRangoReferencia());
        entidad.setUnidadMedida(dto.getUnidadMedida());
        entidad.setLaboratorio(laboratorio);
        entidad.setEstado(dto.getEstado() != null ? dto.getEstado() : 1);
        return toResponseDTO(repository.save(entidad));
    }

    @Transactional
    public ExamenLaboratorioDTO.ResponseDTO actualizar(Integer id, ExamenLaboratorioDTO.CreateDTO dto) {
        validar(dto);
        ExamenLaboratorio entidad = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Examen de laboratorio no encontrado."));
        Laboratorio laboratorio = laboratorioRepository.findById(dto.getLaboratorioId())
                .orElseThrow(() -> new ValidationException("Debe seleccionar un laboratorio."));

        entidad.setCodigo(dto.getCodigo());
        entidad.setNombre(dto.getNombre());
        entidad.setPrecio(dto.getPrecio());
        entidad.setRangoReferencia(dto.getRangoReferencia());
        entidad.setUnidadMedida(dto.getUnidadMedida());
        entidad.setLaboratorio(laboratorio);
        entidad.setEstado(dto.getEstado());
        return toResponseDTO(repository.save(entidad));
    }

    @Transactional
    public void eliminar(Integer id) {
        ExamenLaboratorio entidad = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Examen de laboratorio no encontrado."));
        entidad.setEstado((short) 0);
        repository.save(entidad);
    }

    private void validar(ExamenLaboratorioDTO.CreateDTO dto) {
        if (dto.getPrecio() == null || dto.getPrecio().doubleValue() <= 0) {
            throw new ValidationException("El precio base debe ser mayor a 0.");
        }
        if (dto.getLaboratorioId() == null) {
            throw new ValidationException("Debe seleccionar un laboratorio.");
        }
    }

    private ExamenLaboratorioDTO.ResponseDTO toResponseDTO(ExamenLaboratorio e) {
        ExamenLaboratorioDTO.ResponseDTO dto = new ExamenLaboratorioDTO.ResponseDTO();
        dto.setId(e.getId());
        dto.setCodigo(e.getCodigo());
        dto.setNombre(e.getNombre());
        dto.setPrecio(e.getPrecio());
        dto.setRangoReferencia(e.getRangoReferencia());
        dto.setUnidadMedida(e.getUnidadMedida());
        dto.setLaboratorioId(e.getLaboratorio().getId());
        dto.setLaboratorioNombre(e.getLaboratorio().getNombre());
        dto.setEstado(e.getEstado());
        return dto;
    }
}