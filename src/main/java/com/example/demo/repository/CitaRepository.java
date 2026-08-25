package com.example.demo.repository;

import com.example.demo.modelo.Cita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CitaRepository extends JpaRepository<Cita, Integer> {

    // CU-05, RN-CU05-01: búsqueda por DPI. Cita activa más próxima primero.
    @Query("""
            SELECT c FROM Cita c
            WHERE c.paciente.dpi = :dpi
            ORDER BY
                CASE WHEN c.estadoCita.nombre IN ('Cancelada','Atención Finalizada','No Asistió') THEN 1 ELSE 0 END,
                c.fechaHora DESC
            """)
    List<Cita> buscarPorDpiPaciente(@Param("dpi") String dpi);

    // CU-03: horarios disponibles / doble reserva
    List<Cita> findByMedico_IdAndFechaHoraBetweenAndEstadoCita_NombreNot(
            Integer medicoId, LocalDateTime desde, LocalDateTime hasta, String estadoExcluido);

    // CU-03: job de expiración de reservas (FA03 / RNF-019)
    List<Cita> findByEstadoCita_NombreAndExpiraEnBefore(String estadoNombre, LocalDateTime momento);

    // CU-06, RN-CU06-01: solo citas "Pendiente de pago"
    Optional<Cita> findByIdAndEstadoCita_Nombre(Integer id, String estadoNombre);

    List<Cita> findByPaciente_DpiAndEstadoCita_Nombre(String dpi, String estadoNombre);

    // ------------------------------------------------------------------
    // NUEVOS — CU-07: cola de enfermería (ordenados por hora de llegada)
    // ------------------------------------------------------------------
    List<Cita> findByEstadoCita_NombreOrderByHoraLlegadaAsc(String estadoNombre);

    // ------------------------------------------------------------------
    // NUEVOS — CU-08: panel del médico, filtrado por médico + estado
    // ------------------------------------------------------------------
    List<Cita> findByMedico_IdAndEstadoCita_NombreOrderByFechaHoraAsc(Integer medicoId, String estadoNombre);

    // CU-08 paso 2/3: validar que la cita pertenece al médico antes de operar sobre ella
    Optional<Cita> findByIdAndMedico_Id(Integer id, Integer medicoId);

    // ------------------------------------------------------------------
    // NUEVO — CU-14: Agenda Médica. Citas del médico dentro de un rango,
    // excluyendo Pendiente de pago, No Asistió y Cancelada (no se pintan en el calendario).
    // ------------------------------------------------------------------
    @Query("""
            SELECT c FROM Cita c
            WHERE c.medico.id = :medicoId
            AND c.fechaHora BETWEEN :desde AND :hasta
            AND c.estadoCita.nombre NOT IN ('Pendiente de pago', 'No Asistió', 'Cancelada')
            ORDER BY c.fechaHora ASC
            """)
    List<Cita> findParaAgendaMedico(@Param("medicoId") Integer medicoId,
                                    @Param("desde") LocalDateTime desde,
                                    @Param("hasta") LocalDateTime hasta);

    // ------------------------------------------------------------------
    // NUEVO — Dashboard del Paciente: sus propias citas (historial + estado del proceso).
    // ------------------------------------------------------------------
    List<Cita> findByPaciente_IdOrderByFechaHoraDesc(Integer pacienteId);

    // Valida que la cita consultada pertenezca al paciente autenticado (evita ver citas ajenas).
    Optional<Cita> findByIdAndPaciente_Id(Integer id, Integer pacienteId);
}