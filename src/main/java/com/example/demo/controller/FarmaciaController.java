package com.example.demo.controller;

import com.example.demo.dto.FarmaciaDTOs.*;
import com.example.demo.security.AuthUsuario;
import com.example.demo.service.FarmaciaService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// ============================================================================
// CU-11 DESPACHO DE MEDICAMENTOS  (11_CU_Despacho_de_Medicamentos.docx; en el código y en el RN consolidado figura como CU-10)
// Cómo buscar en este archivo (Ctrl+F / Ctrl+Shift+F):
//   >>> INICIO FB     = donde empieza cada paso del Flujo Básico
//   >>> INICIO FA     = donde empieza cada Flujo Alterno
//   >>> VALIDACION    = donde se valida una regla de negocio (RN) o una precondición
// Este controlador NO valida reglas de negocio: están en FarmaciaService.
//
// FLUJO BÁSICO (FB)                                  ENDPOINT
//   Paso 1       'Nuevo Despacho' (pantalla)         front
//   Paso 2       búsqueda de recetas activas         GET  /recetas/buscar
//   Pasos 3-5    'Despachar', vigencia e inventario  GET  /recetas/{idReceta}
//   Pasos 6-12   ajustes, total, confirmar, resumen  POST /recetas/{idReceta}/despachar
// FLUJOS ALTERNOS (FA)
//   FA01 sin inventario / stock bajo  -> dentro de despachar (alertas) y de GET /recetas/{id} (stockBajo)
//   FA02 sustitución                  -> campos sustituido/razon en el body de despachar
//   FA03 paciente no desea comprar    POST /recetas/{idReceta}/cancelar
//   FA04 stock mínimo tras despacho   -> dentro de despachar (alerta)
// FUERA DE CU-11 (solo viven en este archivo): GET /medicamentos/buscar (venta libre) y POST /inventario/ajustar (ver CU-14 / CU-15).
// Acceso (SecurityConfig): Farmacéutico, Administrador de Sede y Admin General (sede: usuario.sucursalScopeOrNull()).
// ============================================================================
@RestController
@RequestMapping("/api/farmacia")
@RequiredArgsConstructor
public class FarmaciaController {

    private final FarmaciaService farmaciaService;

    // >>> INICIO FB - PASO 2: búsqueda de recetas activas por ID de Receta o ID de Consulta (los pasos 1 y 3 son pantalla/botón del front).
    // GET /api/farmacia/recetas/buscar?tipo=RECETA&valor=5
    // GET /api/farmacia/recetas/buscar?tipo=CONSULTA&valor=12
    // Administrador de Sede: solo ve recetas de citas de su propia sede.
    @GetMapping("/recetas/buscar")
    public List<RecetaBusquedaDTO> buscarRecetas(@RequestParam String tipo, @RequestParam String valor,
                                                 @AuthenticationPrincipal AuthUsuario usuario) {
        return farmaciaService.buscarRecetas(tipo, valor, usuario.sucursalScopeOrNull());
    }

    // >>> INICIO FB - PASOS 3, 4 y 5: detalle de la receta (vigencia = paso 4) + disponibilidad de inventario en la sucursal (paso 5 / FA01)
    @GetMapping("/recetas/{idReceta}")
    public RecetaDetalleDTO obtenerDetalle(@PathVariable Integer idReceta,
                                           @RequestParam(required = false) Integer idSucursal,
                                           @AuthenticationPrincipal AuthUsuario usuario) {
        return farmaciaService.obtenerDetalleReceta(idReceta, idSucursal, usuario.sucursalScopeOrNull());
    }

    // >>> FUERA del CU-11 escrito (venta libre): catálogo/búsqueda de medicamentos con stock, sin necesidad de receta
    // GET /api/farmacia/medicamentos/buscar?nombre=para&idSucursal=1
    @GetMapping("/medicamentos/buscar")
    public List<MedicamentoCatalogoDTO> buscarMedicamentos(@RequestParam(required = false) String nombre,
                                                           @RequestParam(required = false) Integer idSucursal,
                                                           @AuthenticationPrincipal AuthUsuario usuario) {
        return farmaciaService.buscarMedicamentos(nombre, idSucursal, usuario.sucursalScopeOrNull());
    }

    // >>> INICIO FB - PASOS 6 a 12: "Confirmar Despacho" de UNA receta (sustitución FA02, total, registro, resumen).
    // (El CU escrito llega hasta el paso 12; 'Pasos 10-13' era una numeración vieja)
    // No hay cobro dentro del sistema (el cobro físico es externo).
    // El farmacéutico sale del JWT.
    @PostMapping("/recetas/{idReceta}/despachar")
    public ConfirmarDespachoResponseDTO confirmarDespacho(@PathVariable Integer idReceta,
                                                          @RequestBody ConfirmarDespachoRequestDTO request,
                                                          @AuthenticationPrincipal AuthUsuario usuario) {
        return farmaciaService.confirmarDespacho(idReceta, request, usuario.getId(), usuario.sucursalScopeOrNull());
    }

    // >>> INICIO FA03 - El paciente no desea adquirir los medicamentos de una receta
    @PostMapping("/recetas/{idReceta}/cancelar")
    public CancelarDespachoResponseDTO cancelarDespacho(@PathVariable Integer idReceta,
                                                        @AuthenticationPrincipal AuthUsuario usuario) {
        return farmaciaService.cancelarDespacho(idReceta, usuario.sucursalScopeOrNull());
    }

    // >>> FUERA del CU-11 escrito: ajustar / cargar stock de un medicamento (equivale a CU-15 Bitácora, versión simplificada).
    // idUsuario ya no viaja en el body: sale del JWT.
    @PostMapping("/inventario/ajustar")
    public AjustarStockResponseDTO ajustarStock(@RequestBody AjustarStockRequestDTO request,
                                                @AuthenticationPrincipal AuthUsuario usuario) {
        return farmaciaService.ajustarStock(request, usuario.getId(), usuario.sucursalScopeOrNull());
    }
}