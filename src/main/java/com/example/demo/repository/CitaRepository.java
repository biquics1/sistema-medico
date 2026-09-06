// ============================================================
// REPOSITORY: CitaRepository
// Acceso a datos de la entidad Cita: es el repositorio más
// usado del sistema, ya que casi todos los casos de uso
// (CU-03, CU-05, CU-06, CU-07, CU-08, CU-14) consultan citas
// bajo distintos filtros (paciente, médico, estado, fechas).
// ============================================================
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
    // El CASE WHEN manda al final las citas ya cerradas (Cancelada/Atención Finalizada/No Asistió)
    @Query("""
            SELECT c FROM Cita c
            WHERE c.paciente.dpi = :dpi
            ORDER BY
                CASE WHEN c.estadoCita.nombre IN ('Cancelada','Atención Finalizada','No Asistió') THEN 1 ELSE 0 END,
                c.fechaHora DESC
            """)
    List<Cita> buscarPorDpiPaciente(@Param("dpi") String dpi);

    // CU-03: horarios disponibles / doble reserva
    // Busca citas de un médico en un rango de fechas, excluyendo una lista de estados
    // (ej. Cancelada y No Asistió: esas SÍ liberan el horario para volver a agendarse).
    // MODIFICADO: antes solo excluía un estado (NombreNot); ahora excluye varios (NombreNotIn)
    // para que "No Asistió" también libere el horario, igual que "Cancelada".
    List<Cita> findByMedico_IdAndFechaHoraBetweenAndEstadoCita_NombreNotIn(
            Integer medicoId, LocalDateTime desde, LocalDateTime hasta, List<String> estadosExcluidos);

    // NUEVO — Regla de doble reserva (evita que dos pacientes tomen la misma hora
    // del mismo médico): existencia exacta por medico + fecha_hora, excluyendo los
    // estados que "liberan" el horario (Cancelada, No Asistió).
    boolean existsByMedico_IdAndFechaHoraAndEstadoCita_NombreNotIn(
            Integer medicoId, LocalDateTime fechaHora, List<String> estadosExcluidos);

    // NUEVO — Regla de "una cita activa por especialidad por paciente": existencia
    // de otra cita del mismo paciente en la misma especialidad que aún no llegó a
    // un estado terminal (Atención Finalizada, No Asistió, Cancelada).
    boolean existsByPaciente_IdAndEspecialidad_IdAndEstadoCita_NombreNotIn(
            Integer pacienteId, Integer especialidadId, List<String> estadosExcluidos);

    // CU-03: job de expiración de reservas (FA03 / RNF-019)
    // Citas en cierto estado cuya reserva ya expiró (para liberarlas automáticamente)
    List<Cita> findByEstadoCita_NombreAndExpiraEnBefore(String estadoNombre, LocalDateTime momento);

    // CU-06, RN-CU06-01: solo citas "Pendiente de pago"
    Optional<Cita> findByIdAndEstadoCita_Nombre(Integer id, String estadoNombre);

    // Búsqueda de citas por DPI del paciente y estado específico
    List<Cita> findByPaciente_DpiAndEstadoCita_Nombre(String dpi, String estadoNombre);

    // ------------------------------------------------------------------
    // NUEVOS — CU-07: cola de enfermería (ordenados por hora de llegada)
    // ------------------------------------------------------------------
    List<Cita> findByEstadoCita_NombreOrderByHoraLlegadaAsc(String estadoNombre);

    // ------------------------------------------------------------------
    // NUEVOS — CU-08: panel del médico, filtrado por médico + estado
    // ------------------------------------------------------------------
    List<Cita> findByMedico_IdAndEstadoCita_NombreOrderByFechaHoraAsc(Integer medicoId, String estadoNombre);

    // NUEVO — CU-08: panel consolidado para Administrador General (todos los médicos)
    List<Cita> findByEstadoCita_NombreOrderByFechaHoraAsc(String estadoNombre);

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