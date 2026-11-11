package com.example.demo.service;

import com.example.demo.dto.EnfermeriaDTOs.HistorialCitaDTO;
import com.example.demo.dto.EnfermeriaDTOs.SignosVitalesDetalleDTO;
import com.example.demo.modelo.Cita;
import com.example.demo.repository.CitaRepository;
import com.example.demo.repository.ConsultaMedicaRepository;
import com.example.demo.repository.SignosVitalesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Historial de citas de un paciente con sus detalles (signos vitales y diagnóstico).
// Lo comparten enfermería (CU-07) y el médico (CU-08); el control de acceso a la cita
// actual lo hace cada service antes de llamar aquí.
@Service
@RequiredArgsConstructor
public class HistorialPacienteService {

    private final CitaRepository citaRepository;
    private final ConsultaMedicaRepository consultaMedicaRepository;
    private final SignosVitalesRepository signosVitalesRepository;

    // Citas del mismo paciente ANTERIORES a la cita actual (la actual está en curso). Máx. 20.
    @Transactional(readOnly = true)
    public List<HistorialCitaDTO> historialAnteriorA(Cita actual) {
        return citaRepository
                .findTop20ByPaciente_IdAndFechaHoraBeforeOrderByFechaHoraDesc(
                        actual.getPaciente().getId(), actual.getFechaHora())
                .stream()
                .filter(c -> !c.getId().equals(actual.getId()))
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public SignosVitalesDetalleDTO signosDeCita(Integer citaId) {
        return signosVitalesRepository.findByCita_Id(citaId)
                .map(sv -> SignosVitalesDetalleDTO.builder()
                        .presionSistolica(sv.getPresionSistolica())
                        .presionDiastolica(sv.getPresionDiastolica())
                        .temperatura(sv.getTemperatura())
                        .peso(sv.getPeso())
                        .talla(sv.getTalla())
                        .frecuenciaCardiaca(sv.getFrecuenciaCardiaca())
                        .esEmergencia(sv.isEsEmergencia())
                        .registradoPor(sv.getEnfermero().getNombreCompleto())
                        .registradoEn(sv.getCreadoEn())
                        .build())
                .orElse(null);
    }

    private HistorialCitaDTO toDTO(Cita c) {
        String diagnostico = consultaMedicaRepository.findByCita_Id(c.getId())
                .map(cm -> cm.getDiagnostico())
                .filter(d -> !d.isBlank())
                .orElse(null);

        return HistorialCitaDTO.builder()
                .id(c.getId())
                .fechaHora(c.getFechaHora())
                .especialidad(c.getEspecialidad().getNombre())
                .medico(c.getMedico().getNombreCompleto())
                .sucursal(c.getSucursal().getNombre())
                .estado(c.getEstadoCita().getNombre())
                .motivoConsulta(c.getMotivoConsulta())
                .esEmergencia(c.isEsEmergencia())
                .esSeguimiento(c.isEsSeguimiento())
                .diagnostico(diagnostico)
                .signosVitales(signosDeCita(c.getId()))
                .build();
    }
}
