package com.example.demo.controller;

import com.example.demo.dto.CajaDTOs.*;
import com.example.demo.security.AuthUsuario;
import com.example.demo.service.CajaService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

// Módulo de Caja (CU-06 Cobro de Consulta y CU-10 Cobro de Laboratorio en Caja):
// búsqueda de citas/órdenes pendientes de pago y registro del cobro con comprobante.
@RestController
@RequestMapping("/api/caja")
@RequiredArgsConstructor
public class CajaController {

    private final CajaService cajaService;

    // Paso 1-2 FB: GET /api/caja/citas/buscar?tipo=DPI&valor=1234567890123
    //              GET /api/caja/citas/buscar?tipo=CITA&valor=1
    // Administrador de Sede y Cajero: ambos quedan acotados a su propia
    // sucursal (usuario.sucursalScopeOrNull()). Solo Administrador General
    // ve/cobra citas de cualquier sucursal.
    @GetMapping("/citas/buscar")
    public BusquedaCobroResultadoDTO buscar(@RequestParam String tipo, @RequestParam String valor,
                                            @AuthenticationPrincipal AuthUsuario usuario) {
        return cajaService.buscarCitaParaCobro(tipo, valor, usuario.sucursalScopeOrNull());
    }

    // Paso 6-9 FB: POST /api/caja/citas/{idCita}/cobrar
    // idCajero ya no viaja en el body: sale del JWT (rol Cajero/Administrador).
    @PostMapping("/citas/{idCita}/cobrar")
    public ComprobantePagoDTO cobrar(@PathVariable Integer idCita,
                                     @RequestBody CobrarRequestDTO request,
                                     @AuthenticationPrincipal AuthUsuario usuario) {
        return cajaService.cobrar(idCita, request, usuario.getId(), usuario.sucursalScopeOrNull());
    }

    // Cobro de Laboratorio en Caja (precondición RN-CU09-01 de CU-09)
    // GET /api/caja/laboratorio/buscar?tipo=DPI&valor=...  |  tipo=ORDEN&valor=1
    @GetMapping("/laboratorio/buscar")
    public BusquedaCobroLabResultadoDTO buscarOrdenLab(@RequestParam String tipo, @RequestParam String valor,
                                                       @AuthenticationPrincipal AuthUsuario usuario) {
        return cajaService.buscarOrdenLabParaCobro(tipo, valor, usuario.sucursalScopeOrNull());
    }

    // POST /api/caja/laboratorio/{idOrden}/cobrar
    @PostMapping("/laboratorio/{idOrden}/cobrar")
    public ComprobantePagoDTO cobrarLaboratorio(@PathVariable Integer idOrden,
                                                @RequestBody CobrarRequestDTO request,
                                                @AuthenticationPrincipal AuthUsuario usuario) {
        return cajaService.cobrarLaboratorio(idOrden, request, usuario.getId(), usuario.sucursalScopeOrNull());
    }
}
