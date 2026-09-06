package com.example.demo.service;

import com.example.demo.dto.EnfermeriaDTOs.*;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.ValidationException;
import com.example.demo.modelo.Cita;
import com.example.demo.modelo.EstadoCita;
import com.example.demo.modelo.SignosVitales;
import com.example.demo.modelo.Usuario;
import com.example.demo.repository.CitaRepository;
import com.example.demo.repository.EstadoCitaRepository;
import com.example.demo.repository.SignosVitalesRepository;
import com.example.demo.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EnfermeriaService {
    private final AuditoriaContexto auditoriaContexto;

    // Rangos clínicos normales (RN-CU07-06) — distintos del rango de CAPTURA (RN-CU07-01 a 05)
    private static final int PA_SIST_NORMAL_MIN = 90, PA_SIST_NORMAL_MAX = 140;
    private static final int PA_DIAST_NORMAL_MIN = 60, PA_DIAST_NORMAL_MAX = 90;
    private static final BigDecimal TEMP_NORMAL_MIN = new BigDecimal("36.0");
    private static final BigDecimal TEMP_NORMAL_MAX = new BigDecimal("37.5");
    private static final int FC_NORMAL_MIN = 60, FC_NORMAL_MAX = 100;

    private final CitaRepository citaRepository;
    private final UsuarioRepository usuarioRepository;
    private final EstadoCitaRepository estadoCitaRepository;
    private final SignosVitalesRepository signosVitalesRepository;

    // ---------------------------------------------------------------
    // Paso 1 FB: cola de enfermería (Paciente Presente / en proceso)
    // sucursalScope != null -> Administrador de Sede o Enfermero: solo su
    // sucursal. null -> Administrador General: sin restricción.
    // ---------------------------------------------------------------
    @Transactional(readOnly = true)
    public ColaEnfermeriaDTO obtenerCola(Integer sucursalScope) {
        List<PacienteColaDTO> presentes = citaRepository
                .findByEstadoCita_NombreOrderByHoraLlegadaAsc(EstadoCita.PACIENTE_PRESENTE)
                .stream().filter(c -> perteneceASede(c, sucursalScope)).map(this::toDTO).toList();

        List<PacienteColaDTO> enProceso = citaRepository
                .findByEstadoCita_NombreOrderByHoraLlegadaAsc(EstadoCita.SIGNOS_VITALES)
                .stream().filter(c -> perteneceASede(c, sucursalScope)).map(this::toDTO).toList();

        return ColaEnfermeriaDTO.builder()
                .presentes(presentes)
                .enProceso(enProceso)
                .build();
    }

    private boolean perteneceASede(Cita c, Integer sucursalScope) {
        return sucursalScope == null || sucursalScope.equals(c.getSucursal().getId());
    }

    // ---------------------------------------------------------------
    // Paso 1 FB: "Llamar y Tomar Signos" -> Paciente Presente -> Signos Vitales
    // ---------------------------------------------------------------
    @Transactional
    public LlamarPacienteResponseDTO llamarPaciente(Integer idCita, Integer sucursalScope) {
        auditoriaContexto.aplicar();
        Cita cita = citaRepository.findById(idCita)
                .filter(c -> perteneceASede(c, sucursalScope))
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la cita indicada."));

        if (!EstadoCita.PACIENTE_PRESENTE.equals(cita.getEstadoCita().getNombre())) {
            throw new ValidationException(
                    "La cita no se encuentra en estado 'Paciente Presente'. Verifique el estado actual.");
        }

        EstadoCita signosVitales = estadoCitaRepository.findByNombre(EstadoCita.SIGNOS_VITALES)
                .orElseThrow(() -> new ResourceNotFoundException("Catálogo estado_cita incompleto: falta 'Signos Vitales'."));

        cita.setEstadoCita(signosVitales);
        cita.setVecesLlamadoEnfermeria((short) 1); // primer llamado de un máximo de 3
        Cita actualizada = citaRepository.save(cita);

        String mensaje = String.format(
                "Turno número %d. Paciente %s, favor pasar a toma de signos vitales. (Llamado 1 de 3)",
                actualizada.getId(), actualizada.getPaciente().getNombreCompleto());

        return LlamarPacienteResponseDTO.builder()
                .mensajeAnuncio(mensaje)
                .cita(toDTO(actualizada))
                .build();
    }

    // ---------------------------------------------------------------
    // "Llamar de nuevo": reintento de anuncio sin cambiar de estado,
    // hasta un máximo de 3 llamados en total (1 inicial + 2 reintentos).
    // ---------------------------------------------------------------
    @Transactional
    public LlamarPacienteResponseDTO llamarDeNuevo(Integer idCita, Integer sucursalScope) {
        auditoriaContexto.aplicar();
        Cita cita = citaRepository.findById(idCita)
                .filter(c -> perteneceASede(c, sucursalScope))
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la cita indicada."));

        if (!EstadoCita.SIGNOS_VITALES.equals(cita.getEstadoCita().getNombre())) {
            throw new ValidationException("Solo se puede volver a llamar a un paciente que ya fue llamado a signos vitales.");
        }

        short vecesActuales = cita.getVecesLlamadoEnfermeria() == null ? 1 : cita.getVecesLlamadoEnfermeria();
        if (vecesActuales >= 3) {
            throw new ValidationException(
                    "Se alcanzó el máximo de 3 llamados para esta cita. Puede marcar 'No Asistió' para cancelarla.");
        }

        short nuevasVeces = (short) (vecesActuales + 1);
        cita.setVecesLlamadoEnfermeria(nuevasVeces);
        Cita actualizada = citaRepository.save(cita);

        String mensaje = String.format(
                "Turno número %d. Paciente %s, favor pasar a toma de signos vitales. (Llamado %d de 3)",
                actualizada.getId(), actualizada.getPaciente().getNombreCompleto(), nuevasVeces);

        return LlamarPacienteResponseDTO.builder()
                .mensajeAnuncio(mensaje)
                .cita(toDTO(actualizada))
                .build();
    }

    // ---------------------------------------------------------------
    // "No Asistió": el paciente no se presenta a signos vitales pese a
    // los llamados -> se cancela la cita (estado Cancelada).
    // ---------------------------------------------------------------
    @Transactional
    public AccionCitaEnfermeriaResponseDTO marcarNoAsistio(Integer idCita, Integer sucursalScope) {
        auditoriaContexto.aplicar();
        Cita cita = citaRepository.findById(idCita)
                .filter(c -> perteneceASede(c, sucursalScope))
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la cita indicada."));

        if (!EstadoCita.SIGNOS_VITALES.equals(cita.getEstadoCita().getNombre())) {
            throw new ValidationException("Solo se puede marcar 'No Asistió' para un paciente ya llamado a signos vitales.");
        }

        EstadoCita cancelada = estadoCitaRepository.findByNombre(EstadoCita.CANCELADA)
                .orElseThrow(() -> new ResourceNotFoundException("Catálogo estado_cita incompleto: falta 'Cancelada'."));
        cita.setEstadoCita(cancelada);
        Cita actualizada = citaRepository.save(cita);

        return AccionCitaEnfermeriaResponseDTO.builder()
                .mensaje("Cita #" + actualizada.getId() + " cancelada por inasistencia del paciente.")
                .cita(toDTO(actualizada))
                .build();
    }

    // ---------------------------------------------------------------
    // Paso 4-11 FB: registrar signos vitales (FA01, FA02, FA03)
    // ---------------------------------------------------------------
    @Transactional
    public SignosVitalesResponseDTO registrarSignosVitales(Integer idCita, RegistrarSignosVitalesRequestDTO req,
                                                           Integer idEnfermero, Integer sucursalScope) {
        auditoriaContexto.aplicar();
        Cita cita = citaRepository.findById(idCita)
                .filter(c -> perteneceASede(c, sucursalScope))
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la cita indicada."));

        if (!EstadoCita.SIGNOS_VITALES.equals(cita.getEstadoCita().getNombre())) {
            throw new ValidationException(
                    "Debe llamar al paciente (Signos Vitales) antes de registrar sus datos.");
        }
        if (signosVitalesRepository.existsByCita_Id(idCita)) {
            throw new ValidationException("Ya se registraron los signos vitales para esta cita.");
        }

        validarRangosCaptura(req); // FA02

        Usuario enfermero = usuarioRepository.findById(idEnfermero)
                .orElseThrow(() -> new ResourceNotFoundException("El enfermero indicado no existe."));

        SignosVitales sv = new SignosVitales();
        sv.setCita(cita);
        sv.setEnfermero(enfermero);
        sv.setPresionSistolica(req.getPresionSistolica());
        sv.setPresionDiastolica(req.getPresionDiastolica());
        sv.setTemperatura(req.getTemperatura());
        sv.setPeso(req.getPeso());
        sv.setTalla(req.getTalla());
        sv.setFrecuenciaCardiaca(req.getFrecuenciaCardiaca());
        sv.setEsEmergencia(req.isEsEmergencia());
        signosVitalesRepository.save(sv);

        // FA03: alertas de rango clínico (informativas, no bloquean el guardado)
        List<String> alertas = calcularAlertasClinicas(req);

        // FA01: si es emergencia, se refleja también en la cita (para el panel del médico)
        if (req.isEsEmergencia()) {
            cita.setEsEmergencia(true);
        }

        EstadoCita enEspera = estadoCitaRepository.findByNombre(EstadoCita.EN_ESPERA)
                .orElseThrow(() -> new ResourceNotFoundException("Catálogo estado_cita incompleto: falta 'En Espera'."));
        cita.setEstadoCita(enEspera);
        Cita actualizada = citaRepository.save(cita);

        String nombre = actualizada.getPaciente().getNombreCompleto();
        String mensaje = req.isEsEmergencia()
                ? "Signos vitales de emergencia registrados para paciente " + nombre
                + ". El paciente debe pasar directamente a consulta médica."
                : "Signos vitales del paciente " + nombre
                + " registrados correctamente. El paciente puede regresar a la sala de espera.";

        return SignosVitalesResponseDTO.builder()
                .mensaje(mensaje)
                .alertasClinicas(alertas)
                .cita(toDTO(actualizada))
                .build();
    }

    // ---------------------------------------------------------------
    // RN-CU07-01 a RN-CU07-05: rango de captura (bloquea el guardado)
    // ---------------------------------------------------------------
    private void validarRangosCaptura(RegistrarSignosVitalesRequestDTO r) {
        if (r.getPresionSistolica() == null || r.getPresionSistolica() < 60 || r.getPresionSistolica() > 250
                || r.getPresionDiastolica() == null || r.getPresionDiastolica() < 40 || r.getPresionDiastolica() > 150) {
            throw new ValidationException(
                    "La presión arterial debe ingresarse en formato sistólica/diastólica dentro de rangos válidos "
                            + "(sistólica 60-250 mmHg, diastólica 40-150 mmHg).");
        }
        if (r.getTemperatura() == null
                || r.getTemperatura().compareTo(new BigDecimal("34.0")) < 0
                || r.getTemperatura().compareTo(new BigDecimal("42.0")) > 0) {
            throw new ValidationException("La temperatura debe estar entre 34.0 y 42.0°C.");
        }
        if (r.getPeso() == null
                || r.getPeso().compareTo(new BigDecimal("0.5")) < 0
                || r.getPeso().compareTo(new BigDecimal("300")) > 0) {
            throw new ValidationException("El peso debe estar entre 0.5 y 300 kg.");
        }
        if (r.getTalla() == null
                || r.getTalla().compareTo(new BigDecimal("30")) < 0
                || r.getTalla().compareTo(new BigDecimal("250")) > 0) {
            throw new ValidationException("La talla debe estar entre 30 y 250 cm.");
        }
        if (r.getFrecuenciaCardiaca() == null || r.getFrecuenciaCardiaca() < 30 || r.getFrecuenciaCardiaca() > 220) {
            throw new ValidationException("La frecuencia cardíaca debe estar entre 30 y 220 latidos por minuto.");
        }
    }

    // ---------------------------------------------------------------
    // RN-CU07-06: alertas de rango clínico normal (no bloquean el registro)
    // ---------------------------------------------------------------
    private List<String> calcularAlertasClinicas(RegistrarSignosVitalesRequestDTO r) {
        List<String> alertas = new ArrayList<>();

        if (r.getPresionSistolica() < PA_SIST_NORMAL_MIN || r.getPresionSistolica() > PA_SIST_NORMAL_MAX
                || r.getPresionDiastolica() < PA_DIAST_NORMAL_MIN || r.getPresionDiastolica() > PA_DIAST_NORMAL_MAX) {
            alertas.add("Presión arterial fuera de rango normal.");
        }
        if (r.getTemperatura().compareTo(TEMP_NORMAL_MIN) < 0 || r.getTemperatura().compareTo(TEMP_NORMAL_MAX) > 0) {
            alertas.add("Temperatura fuera de rango normal.");
        }
        if (r.getFrecuenciaCardiaca() < FC_NORMAL_MIN || r.getFrecuenciaCardiaca() > FC_NORMAL_MAX) {
            alertas.add("Frecuencia cardíaca fuera de rango normal.");
        }
        return alertas;
    }

    // ---------------------------------------------------------------
    private PacienteColaDTO toDTO(Cita c) {
        return PacienteColaDTO.builder()
                .id(c.getId())
                .nombrePaciente(c.getPaciente().getNombreCompleto())
                .especialidad(c.getEspecialidad().getNombre())
                .sucursal(c.getSucursal().getNombre())
                .estado(c.getEstadoCita().getNombre())
                .esEmergencia(c.isEsEmergencia())
                .fechaHora(c.getFechaHora())
                .horaLlegada(c.getHoraLlegada())
                .vecesLlamado(c.getVecesLlamadoEnfermeria())
                .build();
    }
}