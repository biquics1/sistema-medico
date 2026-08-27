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

// Panel del médico: Consulta Médica (CU-08) y Agendamiento de Cita de
// Seguimiento (CU-11). Cubre desde iniciar consulta hasta generar órdenes de
// laboratorio, recetas y agendar seguimientos.
// El médico actuante ya no viaja como @RequestParam medicoId: sale del JWT
// (rol Médico/Administrador) vía @AuthenticationPrincipal.
@RestController
@RequestMapping("/api/medico")
@RequiredArgsConstructor
public class MedicoController {

    private final ConsultaMedicaService consultaMedicaService;
    private final SeguimientoService seguimientoService;

    // Paso 1 FB
    @GetMapping("/panel")
    public PanelMedicoDTO panel(@AuthenticationPrincipal AuthUsuario usuario) {
        return consultaMedicaService.obtenerPanel(usuario.getId());
    }

    // Paso 2 FB
    @PostMapping("/citas/{idCita}/iniciar-consulta")
    public IniciarConsultaResponseDTO iniciarConsulta(@PathVariable Integer idCita,
                                                      @AuthenticationPrincipal AuthUsuario usuario) {
        return consultaMedicaService.iniciarConsulta(idCita, usuario.getId());
    }

    // Paso 3 FB: contexto para abrir/reabrir el formulario
    @GetMapping("/citas/{idCita}/consulta")
    public ConsultaContextoDTO contexto(@PathVariable Integer idCita,
                                        @AuthenticationPrincipal AuthUsuario usuario) {
        return consultaMedicaService.obtenerContexto(idCita, usuario.getId());
    }

    // Pasos 4-9 FB: guardar borrador o finalizar
    @PutMapping("/citas/{idCita}/consulta")
    public GuardarConsultaResponseDTO guardarConsulta(@PathVariable Integer idCita,
                                                      @RequestBody GuardarConsultaRequestDTO request,
                                                      @AuthenticationPrincipal AuthUsuario usuario) {
        return consultaMedicaService.guardarConsulta(idCita, usuario.getId(), request);
    }

    // FA06
    @PostMapping("/citas/{idCita}/no-asistio")
    public AccionCitaResponseDTO noAsistio(@PathVariable Integer idCita,
                                           @AuthenticationPrincipal AuthUsuario usuario) {
        return consultaMedicaService.marcarNoAsistio(idCita, usuario.getId());
    }

    // Sección "Evaluados"
    @PostMapping("/citas/{idCita}/finalizar-atencion")
    public AccionCitaResponseDTO finalizarAtencion(@PathVariable Integer idCita,
                                                   @AuthenticationPrincipal AuthUsuario usuario) {
        return consultaMedicaService.finalizarAtencion(idCita, usuario.getId());
    }

    // Paso 6 FB: autocompletado CIE-10
    @GetMapping("/cie10")
    public List<Cie10DTO> cie10(@RequestParam String query) {
        return consultaMedicaService.buscarCie10(query);
    }

    // FA01: catálogo de exámenes para el formulario de orden de laboratorio
    @GetMapping("/examenes")
    public List<ExamenCatalogoDTO> examenes() {
        return consultaMedicaService.listarExamenes();
    }

    // FA04: catálogo de medicamentos para el formulario de receta
    @GetMapping("/medicamentos")
    public List<MedicamentoCatalogoDTO> medicamentos() {
        return consultaMedicaService.listarMedicamentos();
    }

    // FA01
    @PostMapping("/consultas/{idConsulta}/orden-laboratorio")
    public OrdenLaboratorioResponseDTO ordenLaboratorio(@PathVariable Integer idConsulta,
                                                        @RequestBody OrdenLaboratorioRequestDTO request,
                                                        @AuthenticationPrincipal AuthUsuario usuario) {
        return consultaMedicaService.generarOrdenLaboratorio(idConsulta, usuario.getId(), request);
    }

    // FA04
    @PostMapping("/consultas/{idConsulta}/receta")
    public RecetaResponseDTO receta(@PathVariable Integer idConsulta,
                                    @RequestBody RecetaRequestDTO request,
                                    @AuthenticationPrincipal AuthUsuario usuario) {
        return consultaMedicaService.generarReceta(idConsulta, usuario.getId(), request);
    }

    //  Agendamiento de Cita de Seguimiento

    // Paso 2-3 FB: banner con los datos precargados (paciente, médico, especialidad, sucursal).
    // El calendario de disponibilidad (paso 5) reutiliza el mismo endpoint de CU-03:
    // GET /api/doctors/{medicoId}/available-slots?date=YYYY-MM-DD
    @GetMapping("/consultas/{idConsulta}/seguimiento/contexto")
    public ContextoSeguimientoDTO contextoSeguimiento(@PathVariable Integer idConsulta,
                                                      @AuthenticationPrincipal AuthUsuario usuario) {
        return seguimientoService.obtenerContexto(idConsulta, usuario.getId());
    }

    // Paso 7 FB: confirmar el agendamiento de la cita de seguimiento
    @PostMapping("/consultas/{idConsulta}/seguimiento")
    public SeguimientoResponseDTO crearSeguimiento(@PathVariable Integer idConsulta,
                                                   @RequestBody CrearSeguimientoRequestDTO request,
                                                   @AuthenticationPrincipal AuthUsuario usuario) {
        return seguimientoService.crearSeguimiento(idConsulta, usuario.getId(), request);
    }
}
