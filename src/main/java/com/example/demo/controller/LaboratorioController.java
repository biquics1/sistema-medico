package com.example.demo.controller;

import com.example.demo.dto.LaboratorioDTOs.*;
import com.example.demo.security.AuthUsuario;
import com.example.demo.service.LaboratorioService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/laboratorio")
@RequiredArgsConstructor
public class LaboratorioController {

    private final LaboratorioService laboratorioService;

    // Paso 1 FB: GET /api/laboratorio/ordenes?estado=1&pacienteId=&medicoId=
    // Administrador de Sede o Laboratorista: solo ven órdenes cuyo médico
    // solicitante es de su sede. Administrador General: sin restricción.
    @GetMapping("/ordenes")
    public List<OrdenListaDTO> listarOrdenes(@RequestParam(required = false) Short estado,
                                              @RequestParam(required = false) Integer pacienteId,
                                              @RequestParam(required = false) Integer medicoId,
                                              @AuthenticationPrincipal AuthUsuario usuario) {
        return laboratorioService.listarOrdenes(estado, pacienteId, medicoId, usuario.sucursalScopeOrNull());
    }

    // Paso 2-3 FB
    @GetMapping("/ordenes/{idOrden}")
    public OrdenDetalleDTO detalle(@PathVariable Integer idOrden, @AuthenticationPrincipal AuthUsuario usuario) {
        return laboratorioService.obtenerDetalle(idOrden, usuario.sucursalScopeOrNull());
    }

    // Pasos 9-10 FB (RN-CU09-02)
    @PutMapping("/ordenes/{idOrden}/examenes/{idDetalle}/resultado")
    public ResultadoResponseDTO registrarResultado(@PathVariable Integer idOrden,
                                                    @PathVariable Integer idDetalle,
                                                    @RequestBody ResultadoRequestDTO request,
                                                    @AuthenticationPrincipal AuthUsuario usuario) {
        return laboratorioService.registrarResultado(idOrden, idDetalle, request, usuario.sucursalScopeOrNull());
    }

    // Pasos 11-13 FB
    @PostMapping("/ordenes/{idOrden}/examenes/{idDetalle}/publicar")
    public PublicarResponseDTO publicar(@PathVariable Integer idOrden, @PathVariable Integer idDetalle,
                                         @AuthenticationPrincipal AuthUsuario usuario) {
        return laboratorioService.publicarResultado(idOrden, idDetalle, usuario.sucursalScopeOrNull());
    }
}
