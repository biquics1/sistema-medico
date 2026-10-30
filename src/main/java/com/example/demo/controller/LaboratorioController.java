package com.example.demo.controller;

import com.example.demo.dto.LaboratorioDTOs.*;
import com.example.demo.security.AuthUsuario;
import com.example.demo.service.LaboratorioService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// ============================================================================
// CU-09 GESTIÓN DE LABORATORIO  (9_CU_Gestion_de_Laboratorio.docx)  -  controlador /api/laboratorio
// Cómo buscar en este archivo (Ctrl+F / Ctrl+Shift+F):
//   >>> INICIO FB     = donde empieza cada paso del Flujo Básico
//   >>> INICIO FA     = donde empieza cada Flujo Alterno
//   >>> VALIDACION    = donde se valida una regla de negocio (RN) o una precondición
// Este controlador NO valida reglas de negocio: están en LaboratorioService.
//
// FLUJO BÁSICO (FB)                                  ENDPOINT
//   Paso 1       tabla 'Órdenes de Laboratorio'      GET  /ordenes
//   Pasos 2-3    detalle de la orden                 GET  /ordenes/{idOrden}
//   Pasos 4-8    (fuera de este controlador) informar monto, cobro en caja (CajaController.cobrarLaboratorio,
//                CU-10 'Cobro de Laboratorio en Caja') -> orden 'En proceso', toma de muestras y análisis
//   Pasos 9-10   guardar resultado de un examen      PUT  /ordenes/{idOrden}/examenes/{idDetalle}/resultado
//   Pasos 11-14  publicar resultado (individual)     POST /ordenes/{idOrden}/examenes/{idDetalle}/publicar
// FLUJOS ALTERNOS (FA)
//   FA01 orden externa  -> la marca nace en CU-08 (FA01); aquí solo se muestra (campo esExterna)
//   FA02 fuera de rango -> se marca en el PUT de resultado (casilla manual 'fueraDeRango')
// Acceso (SecurityConfig): Laboratorista, Administrador de Sede y Admin General (sede: usuario.sucursalScopeOrNull()).
// ============================================================================
@RestController
@RequestMapping("/api/laboratorio")
@RequiredArgsConstructor
public class LaboratorioController {

    private final LaboratorioService laboratorioService;

    // >>> INICIO FB - PASO 1 (y FA01 visual): GET /api/laboratorio/ordenes?estado=1&pacienteId=&medicoId=
    // Administrador de Sede o Laboratorista: solo ven órdenes cuyo médico
    // solicitante es de su sede. Administrador General: sin restricción.
    @GetMapping("/ordenes")
    public List<OrdenListaDTO> listarOrdenes(@RequestParam(required = false) Short estado,
                                             @RequestParam(required = false) Integer pacienteId,
                                             @RequestParam(required = false) Integer medicoId,
                                             @AuthenticationPrincipal AuthUsuario usuario) {
        return laboratorioService.listarOrdenes(estado, pacienteId, medicoId, usuario.sucursalScopeOrNull());
    }

    // >>> INICIO FB - PASOS 2 y 3: seleccionar una orden -> detalle con resumen y lista de exámenes
    @GetMapping("/ordenes/{idOrden}")
    public OrdenDetalleDTO detalle(@PathVariable Integer idOrden, @AuthenticationPrincipal AuthUsuario usuario) {
        return laboratorioService.obtenerDetalle(idOrden, usuario.sucursalScopeOrNull());
    }

    // >>> INICIO FB - PASOS 9 y 10 (RN-CU09-02) y FA02: guardar el resultado de UN examen
    @PutMapping("/ordenes/{idOrden}/examenes/{idDetalle}/resultado")
    public ResultadoResponseDTO registrarResultado(@PathVariable Integer idOrden,
                                                   @PathVariable Integer idDetalle,
                                                   @RequestBody ResultadoRequestDTO request,
                                                   @AuthenticationPrincipal AuthUsuario usuario) {
        return laboratorioService.registrarResultado(idOrden, idDetalle, request, usuario.sucursalScopeOrNull());
    }

    // >>> INICIO FB - PASOS 11 a 14: 'Publicar resultado' (individual, no masivo); al publicar el último, la orden queda Completada
    @PostMapping("/ordenes/{idOrden}/examenes/{idDetalle}/publicar")
    public PublicarResponseDTO publicar(@PathVariable Integer idOrden, @PathVariable Integer idDetalle,
                                        @AuthenticationPrincipal AuthUsuario usuario) {
        return laboratorioService.publicarResultado(idOrden, idDetalle, usuario.sucursalScopeOrNull());
    }
}
