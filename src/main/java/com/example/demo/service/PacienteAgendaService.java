package com.example.demo.service;

import com.example.demo.dto.PacienteDTOs.*;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.modelo.*;
import com.example.demo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PacienteAgendaService {

    private final CitaRepository citaRepository;
    private final ConsultaMedicaRepository consultaMedicaRepository;
    private final OrdenLaboratorioRepository ordenLaboratorioRepository;
    private final DetalleOrdenLaboratorioRepository detalleOrdenLaboratorioRepository;
    private final RecetaMedicaRepository recetaMedicaRepository;
    private final DetalleRecetaMedicaRepository detalleRecetaMedicaRepository;

    public List<MiCitaResumenDTO> listarMisCitas(Integer pacienteId) {
        return citaRepository.findByPaciente_IdOrderByFechaHoraDesc(pacienteId).stream()
                .map(this::toResumenDTO)
                .collect(Collectors.toList());
    }

    public MiCitaDetalleDTO obtenerDetalle(Integer citaId, Integer pacienteId) {
        Cita cita = citaRepository.findByIdAndPaciente_Id(citaId, pacienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada."));

        MiCitaDetalleDTO dto = new MiCitaDetalleDTO();
        dto.setId(cita.getId());
        dto.setEspecialidad(cita.getEspecialidad().getNombre());
        dto.setMedico(cita.getMedico().getNombreCompleto());
        dto.setSucursal(cita.getSucursal().getNombre());
        dto.setFechaHora(cita.getFechaHora());
        dto.setEstado(cita.getEstadoCita().getNombre());
        dto.setMotivoConsulta(cita.getMotivoConsulta());
        dto.setMonto(cita.getMonto());
        dto.setRecetas(Collections.emptyList());

        consultaMedicaRepository.findByCita_Id(citaId).ifPresent(consulta -> {
            ConsultaResumenDTO c = new ConsultaResumenDTO();
            c.setMotivoVisita(consulta.getMotivoVisita());
            c.setHallazgosClinicos(consulta.getHallazgosClinicos());
            c.setDiagnostico(consulta.getDiagnostico());
            c.setPlanTratamiento(consulta.getPlanTratamiento());
            c.setFinalizada(consulta.isFinalizada());
            dto.setConsulta(c);

            ordenLaboratorioRepository.findByConsulta_Id(consulta.getId())
                    .ifPresent(orden -> dto.setOrdenLaboratorio(toOrdenDTO(orden)));

            List<RecetaMedica> recetas = recetaMedicaRepository.findByConsulta_IdAndEstado(consulta.getId(), (short) 1);
            dto.setRecetas(recetas.stream().map(this::toRecetaDTO).collect(Collectors.toList()));
        });

        return dto;
    }

    private OrdenLaboratorioResumenDTO toOrdenDTO(OrdenLaboratorio orden) {
        OrdenLaboratorioResumenDTO o = new OrdenLaboratorioResumenDTO();
        o.setId(orden.getId());
        o.setEstado(orden.getEstado());
        o.setEstadoNombre(nombreEstadoOrden(orden.getEstado()));
        o.setMontoTotal(orden.getMontoTotal());

        List<DetalleOrdenLaboratorio> detalles = detalleOrdenLaboratorioRepository.findByOrden_Id(orden.getId());
        o.setExamenes(detalles.stream().map(d -> {
            ExamenResultadoDTO e = new ExamenResultadoDTO();
            e.setNombreExamen(d.getExamen().getNombre());
            e.setPublicado(d.isPublicado());
            // RN-CU09-02: solo se muestra el resultado si ya fue publicado por el laboratorista.
            if (d.isPublicado()) {
                e.setValorResultado(d.getValorResultado());
                e.setUnidad(d.getUnidad());
                e.setRangoReferencia(d.getExamen().getRangoReferencia());
                e.setFueraDeRango(d.isFueraDeRango());
            }
            return e;
        }).collect(Collectors.toList()));

        return o;
    }

    private RecetaResumenDTO toRecetaDTO(RecetaMedica receta) {
        RecetaResumenDTO r = new RecetaResumenDTO();
        r.setId(receta.getId());
        r.setNotas(receta.getNotas());

        List<DetalleRecetaMedica> detalles = detalleRecetaMedicaRepository.findByReceta_Id(receta.getId());
        r.setMedicamentos(detalles.stream().map(d -> {
            MedicamentoRecetaDTO m = new MedicamentoRecetaDTO();
            m.setNombreMedicamento(d.getMedicamento().getNombre());
            m.setDosis(d.getDosis());
            m.setFrecuencia(d.getFrecuencia());
            m.setDuracion(d.getDuracion());
            m.setIndicaciones(d.getIndicaciones());
            return m;
        }).collect(Collectors.toList()));

        return r;
    }

    private String nombreEstadoOrden(Short estado) {
        if (estado == null) return "Desconocido";
        return switch (estado) {
            case 0 -> "Pendiente";
            case 1 -> "En proceso";
            case 2 -> "Completada";
            case 3 -> "Cancelada";
            default -> "Desconocido";
        };
    }

    private MiCitaResumenDTO toResumenDTO(Cita c) {
        MiCitaResumenDTO dto = new MiCitaResumenDTO();
        dto.setId(c.getId());
        dto.setEspecialidad(c.getEspecialidad().getNombre());
        dto.setMedico(c.getMedico().getNombreCompleto());
        dto.setSucursal(c.getSucursal().getNombre());
        dto.setFechaHora(c.getFechaHora());
        dto.setEstado(c.getEstadoCita().getNombre());
        return dto;
    }
}