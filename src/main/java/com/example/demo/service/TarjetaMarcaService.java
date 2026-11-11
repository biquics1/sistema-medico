package com.example.demo.service;

import com.example.demo.dto.PageResponseDTO;
import com.example.demo.dto.TarjetaMarcaDTO;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.ValidationException;
import com.example.demo.modelo.TarjetaMarca;
import com.example.demo.repository.TarjetaMarcaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

// Catálogo de marcas de tarjeta: detección de marca por número (CU-04),
// validación de marca activa para caja (CU-06), contador de usos y CRUD (CU-14).
@Service
@RequiredArgsConstructor
public class TarjetaMarcaService {

    private final AuditoriaContexto auditoriaContexto;
    private final TarjetaMarcaRepository repository;

    // ---------- Uso en pagos ----------

    public static final String MARCA_COMODIN = "OTRA";

    // CU-04: determina la marca a partir del número completo. NUNCA rechaza: si no coincide con
    // una marca específica activa usa la marca comodín "OTRA"; si tampoco está activa devuelve
    // null (el pago igual se procesa, solo que sin marca para el contador).
    @Transactional(readOnly = true)
    public TarjetaMarca detectarMarca(String numeroSinEspacios) {
        List<TarjetaMarca> activas = repository.findByEstadoOrderByNombreAsc((short) 1);
        return activas.stream()
                .filter(m -> !MARCA_COMODIN.equalsIgnoreCase(m.getNombre()))
                .filter(m -> coincide(m.getPatron(), numeroSinEspacios))
                .findFirst()
                .orElseGet(() -> activas.stream()
                        .filter(m -> MARCA_COMODIN.equalsIgnoreCase(m.getNombre()))
                        .findFirst().orElse(null));
    }

    // CU-06: el cajero elige VISA/MASTERCARD; debe existir y estar activa en el catálogo.
    @Transactional(readOnly = true)
    public TarjetaMarca obtenerActivaPorNombre(String nombre) {
        return repository.findByNombreIgnoreCaseAndEstado(nombre, (short) 1)
                .orElseThrow(() -> new ValidationException(
                        "La marca de tarjeta " + nombre + " no está disponible actualmente."));
    }

    // Debe llamarse en la MISMA transacción que guarda el pago aprobado.
    @Transactional
    public void registrarUso(TarjetaMarca marca) {
        repository.incrementarUsos(marca.getId());
    }

    private boolean coincide(String patron, String numero) {
        try {
            return Pattern.compile(patron).matcher(numero).matches();
        } catch (PatternSyntaxException e) {
            return false;
        }
    }

    // ---------- CRUD del catálogo (CU-14) ----------

    @Transactional(readOnly = true)
    public PageResponseDTO<TarjetaMarcaDTO.ResponseDTO> listar(String filtro, Pageable pageable) {
        Page<TarjetaMarca> page = (filtro == null || filtro.isBlank())
                ? repository.findAll(pageable)
                : repository.findAll((root, q, cb) ->
                cb.like(cb.lower(root.get("nombre")), "%" + filtro.toLowerCase() + "%"), pageable);
        List<TarjetaMarcaDTO.ResponseDTO> contenido = page.getContent().stream().map(this::toDTO).toList();
        return new PageResponseDTO<>(contenido, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    @Transactional
    public TarjetaMarcaDTO.ResponseDTO crear(TarjetaMarcaDTO.CreateDTO dto) {
        auditoriaContexto.aplicar();
        String nombre = dto.getNombre().trim().toUpperCase();
        validarPatron(dto.getPatron());
        if (repository.existsByNombreIgnoreCase(nombre)) {
            throw new ValidationException("Ya existe una marca con el nombre " + nombre + ".");
        }
        TarjetaMarca t = new TarjetaMarca();
        t.setNombre(nombre);
        t.setDescripcion(dto.getDescripcion());
        t.setPatron(dto.getPatron().trim());
        t.setEstado(dto.getEstado());
        return toDTO(repository.save(t));
    }

    @Transactional
    public TarjetaMarcaDTO.ResponseDTO actualizar(Integer id, TarjetaMarcaDTO.CreateDTO dto) {
        auditoriaContexto.aplicar();
        TarjetaMarca t = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Marca de tarjeta no encontrada."));
        String nombre = dto.getNombre().trim().toUpperCase();
        // El nombre lo usa Caja (VISA/MASTERCARD) y el histórico de pagos: no se renombra.
        if (!t.getNombre().equals(nombre)) {
            throw new ValidationException("El nombre de la marca no se puede modificar.");
        }
        validarPatron(dto.getPatron());
        t.setDescripcion(dto.getDescripcion());
        t.setPatron(dto.getPatron().trim());
        t.setEstado(dto.getEstado());
        return toDTO(repository.save(t));
    }

    // Baja lógica: el histórico de pagos y el contador se conservan.
    @Transactional
    public void eliminar(Integer id) {
        auditoriaContexto.aplicar();
        TarjetaMarca t = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Marca de tarjeta no encontrada."));
        t.setEstado((short) 0);
        repository.save(t);
    }

    private void validarPatron(String patron) {
        try {
            Pattern.compile(patron.trim());
        } catch (PatternSyntaxException e) {
            throw new ValidationException("El patrón del número de tarjeta no es una expresión regular válida.");
        }
    }

    private TarjetaMarcaDTO.ResponseDTO toDTO(TarjetaMarca t) {
        TarjetaMarcaDTO.ResponseDTO dto = new TarjetaMarcaDTO.ResponseDTO();
        dto.setId(t.getId());
        dto.setNombre(t.getNombre());
        dto.setDescripcion(t.getDescripcion());
        dto.setPatron(t.getPatron());
        dto.setTotalUsos(t.getTotalUsos());
        dto.setEstado(t.getEstado());
        return dto;
    }
}
