package com.example.demo.controller;

import com.example.demo.dto.ConsultaMedicaDTOs.*;
import com.example.demo.dto.SeguimientoDTOs.*;
import com.example.demo.security.AuthUsuario;
import com.example.demo.service.ConsultaMedicaService;
import com.example.demo.service.SeguimientoService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// ============================================================================
// CU-08 CONSULTA MÉDICA  (8_CU_Consulta_Medica.docx)  -  controlador /api/medico
// También expone los endpoints de CU-12 Cita de Seguimiento (al final del archivo).
// Cómo buscar en este archivo (Ctrl+F / Ctrl+Shift+F):
//   >>> INICIO FB     = donde empieza cada paso del Flujo Básico
//   >>> INICIO FA     = donde empieza cada Flujo Alterno
//   >>> VALIDACION    = donde se valida una regla de negocio (RN) o una precondición
// Este controlador NO valida reglas de negocio: todas están en ConsultaMedicaService.
//
// FLUJO BÁSICO (FB)                              ENDPOINT
//   Paso 1      panel con 3 secciones            GET  /panel
//   Paso 2      Iniciar Consulta                 POST /citas/{id}/iniciar-consulta
//   Paso 3      Ver / Completar Consulta         GET  /citas/{id}/consulta
//   Pasos 4-9   formulario, guardar / finalizar  PUT  /citas/{id}/consulta
//   Paso 7      autocompletado CIE-10            GET  /cie10
//   Pasos 11-12 Evaluados -> Finalizar Atención  POST /citas/{id}/finalizar-atencion
// FLUJOS ALTERNOS (FA)
//   FA01 orden de laboratorio   POST /consultas/{id}/orden-laboratorio  (catálogo: GET /examenes)
//   FA02 cita de seguimiento    POST /consultas/{id}/seguimiento        (es CU-12, SeguimientoService)
//   FA04 receta médica          POST /consultas/{id}/receta             (catálogo: GET /medicamentos)
//   FA05 finalizar sin diagnóstico -> se valida dentro del PUT de guardar consulta (service)
//   FA06 paciente no asistió    POST /citas/{id}/no-asistio
//   (extra, no está en el CU escrito) Llamar de nuevo: POST /citas/{id}/llamar-de-nuevo
//
// El médico actuante ya no viaja como @RequestParam medicoId: sale del JWT
// (rol Médico/Administrador) vía @AuthenticationPrincipal.
// Acceso (SecurityConfig): /api/medico/** -> Médico y Administrador General.
// ============================================================================
@RestController
@RequestMapping("/api/medico")
@RequiredArgsConstructor
public class MedicoController {

    private final ConsultaMedicaService consultaMedicaService;
    private final SeguimientoService seguimientoService;

    // >>> INICIO FB - PASO 1: panel del médico (En Espera / En Consulta Médica / Evaluados)
    @GetMapping("/panel")
    public PanelMedicoDTO panel(@AuthenticationPrincipal AuthUsuario usuario) {
        return consultaMedicaService.obtenerPanel(usuario);
    }

    // >>> INICIO FB - PASO 2: 'Iniciar Consulta' (En Espera -> Consulta Médica + texto del anuncio TTS)
    @PostMapping("/citas/{idCita}/iniciar-consulta")
    public IniciarConsultaResponseDTO iniciarConsulta(@PathVariable Integer idCita,
                                                      @AuthenticationPrincipal AuthUsuario usuario) {
        return consultaMedicaService.iniciarConsulta(idCita, usuario);
    }

    // >>> INICIO FB - PASOS 3 y 4: 'Ver / Completar Consulta' -> abre el formulario (crea o reabre el borrador)
    @GetMapping("/citas/{idCita}/consulta")
    public ConsultaContextoDTO contexto(@PathVariable Integer idCita,
                                        @AuthenticationPrincipal AuthUsuario usuario) {
        return consultaMedicaService.obtenerContexto(idCita, usuario);
    }

    // >>> INICIO FB - PASOS 4 a 9: guardar borrador ('En curso') o finalizar ('Finalizada').
    // Aquí también se dispara FA05 (finalizar sin diagnóstico). Validaciones en ConsultaMedicaService.guardarConsulta
    @PutMapping("/citas/{idCita}/consulta")
    public GuardarConsultaResponseDTO guardarConsulta(@PathVariable Integer idCita,
                                                      @RequestBody GuardarConsultaRequestDTO request,
                                                      @AuthenticationPrincipal AuthUsuario usuario) {
        return consultaMedicaService.guardarConsulta(idCita, usuario, request);
    }

    // >>> EXTRA (no está en el CU escrito) - "Llamar de nuevo": reintento de anuncio sin cambiar de estado (máximo 3 llamados en total)
    @PostMapping("/citas/{idCita}/llamar-de-nuevo")
    public IniciarConsultaResponseDTO llamarDeNuevo(@PathVariable Integer idCita,
                                                    @AuthenticationPrincipal AuthUsuario usuario) {
        return consultaMedicaService.llamarDeNuevo(idCita, usuario);
    }

    // >>> INICIO FA06 - Paciente no asistió. Se ejecuta desde la columna "En Consulta Médica" y cancela la cita
    // (OJO: el CU escrito dice estado 'No Asistió' desde 'En Espera'; el código la pasa a 'Cancelada')
    @PostMapping("/citas/{idCita}/no-asistio")
    public AccionCitaResponseDTO noAsistio(@PathVariable Integer idCita,
                                           @AuthenticationPrincipal AuthUsuario usuario) {
        return consultaMedicaService.marcarNoAsistio(idCita, usuario);
    }

    // >>> INICIO FB - PASOS 11 y 12: sección "Evaluados" -> 'Finalizar Atención' (Evaluado -> Atención Finalizada)
    @PostMapping("/citas/{idCita}/finalizar-atencion")
    public AccionCitaResponseDTO finalizarAtencion(@PathVariable Integer idCita,
                                                   @AuthenticationPrincipal AuthUsuario usuario) {
        return consultaMedicaService.finalizarAtencion(idCita, usuario);
    }

    // >>> INICIO FB - PASO 7: autocompletado CIE-10 (el CU escrito lo numera como paso 7)
    @GetMapping("/cie10")
    public List<Cie10DTO> cie10(@RequestParam String query) {
        return consultaMedicaService.buscarCie10(query);
    }

    // >>> FA01 (paso previo): catálogo de exámenes para el formulario de orden de laboratorio
    @GetMapping("/examenes")
    public List<ExamenCatalogoDTO> examenes() {
        return consultaMedicaService.listarExamenes();
    }

    // >>> FA04 (paso previo): catálogo de medicamentos para el formulario de receta
    @GetMapping("/medicamentos")
    public List<MedicamentoCatalogoDTO> medicamentos() {
        return consultaMedicaService.listarMedicamentos();
    }

    // >>> INICIO FA01 - Generar orden de laboratorio ('Guardar orden').
    // Validaciones en ConsultaMedicaService.generarOrdenLaboratorio. La orden queda 'Pendiente' y continúa en CU-09 / cobro en caja
    @PostMapping("/consultas/{idConsulta}/orden-laboratorio")
    public OrdenLaboratorioResponseDTO ordenLaboratorio(@PathVariable Integer idConsulta,
                                                        @RequestBody OrdenLaboratorioRequestDTO request,
                                                        @AuthenticationPrincipal AuthUsuario usuario) {
        return consultaMedicaService.generarOrdenLaboratorio(idConsulta, usuario, request);
    }

    // >>> INICIO FA04 - Generar receta médica ('Guardar receta').
    // Validaciones en ConsultaMedicaService.generarReceta (RN-CU08-03). La receta luego se despacha en CU-11
    @PostMapping("/consultas/{idConsulta}/receta")
    public RecetaResponseDTO receta(@PathVariable Integer idConsulta,
                                    @RequestBody RecetaRequestDTO request,
                                    @AuthenticationPrincipal AuthUsuario usuario) {
        return consultaMedicaService.generarReceta(idConsulta, usuario, request);
    }

    // >>> INICIO FA02 de CU-08 = CU-12 'Agendamiento de Cita de Seguimiento' (en el código figura como CU-11).
    // La lógica y validaciones de estos 2 endpoints están en SeguimientoService (no es parte de los CU pedidos).
    // Paso 2-3 FB: banner con los datos precargados (paciente, médico, especialidad, sucursal).
    // El calendario de disponibilidad (paso 5) reutiliza el mismo endpoint de CU-03:
    // GET /api/doctors/{medicoId}/available-slots?date=YYYY-MM-DD
    @GetMapping("/consultas/{idConsulta}/seguimiento/contexto")
    public ContextoSeguimientoDTO contextoSeguimiento(@PathVariable Integer idConsulta,
                                                      @AuthenticationPrincipal AuthUsuario usuario) {
        return seguimientoService.obtenerContexto(idConsulta, usuario);
    }

    // Paso 7 FB: confirmar el agendamiento de la cita de seguimiento
    @PostMapping("/consultas/{idConsulta}/seguimiento")
    public SeguimientoResponseDTO crearSeguimiento(@PathVariable Integer idConsulta,
                                                   @RequestBody CrearSeguimientoRequestDTO request,
                                                   @AuthenticationPrincipal AuthUsuario usuario) {
        return seguimientoService.crearSeguimiento(idConsulta, usuario, request);
    }
}