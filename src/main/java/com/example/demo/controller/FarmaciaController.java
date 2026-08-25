package com.example.demo.controller;

import com.example.demo.dto.FarmaciaDTOs.*;
import com.example.demo.security.AuthUsuario;
import com.example.demo.service.FarmaciaService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/farmacia")
@RequiredArgsConstructor
public class FarmaciaController {

    private final FarmaciaService farmaciaService;

    // GET /api/farmacia/recetas/buscar?tipo=RECETA&valor=5
    // GET /api/farmacia/recetas/buscar?tipo=CONSULTA&valor=12
    // Administrador de Sede: solo ve recetas de citas de su propia sede.
    @GetMapping("/recetas/buscar")
    public List<RecetaBusquedaDTO> buscarRecetas(@RequestParam String tipo, @RequestParam String valor,
                                                  @AuthenticationPrincipal AuthUsuario usuario) {
        return farmaciaService.buscarRecetas(tipo, valor, usuario.sucursalScopeOrNull());
    }

    // Detalle de receta + disponibilidad de inventario en la sucursal indicada
    @GetMapping("/recetas/{idReceta}")
    public RecetaDetalleDTO obtenerDetalle(@PathVariable Integer idReceta,
                                            @RequestParam(required = false) Integer idSucursal,
                                            @AuthenticationPrincipal AuthUsuario usuario) {
        return farmaciaService.obtenerDetalleReceta(idReceta, idSucursal, usuario.sucursalScopeOrNull());
    }

    // Catálogo/búsqueda de medicamentos con stock, sin necesidad de receta
    // GET /api/farmacia/medicamentos/buscar?nombre=para&idSucursal=1
    @GetMapping("/medicamentos/buscar")
    public List<MedicamentoCatalogoDTO> buscarMedicamentos(@RequestParam(required = false) String nombre,
                                                             @RequestParam(required = false) Integer idSucursal,
                                                             @AuthenticationPrincipal AuthUsuario usuario) {
        return farmaciaService.buscarMedicamentos(nombre, idSucursal, usuario.sucursalScopeOrNull());
    }

    // Confirmar y pagar el carrito (mezcla ítems con receta y sin receta).
    // idFarmaceutico ya no viaja en el body: sale del JWT (rol Farmacéutico/Administrador).
    @PostMapping("/carrito/pagar")
    public ConfirmarCarritoResponseDTO confirmarCarrito(@RequestBody ConfirmarCarritoRequestDTO request,
                                                         @AuthenticationPrincipal AuthUsuario usuario) {
        return farmaciaService.confirmarCarrito(request, usuario.getId(), usuario.sucursalScopeOrNull());
    }

    // FA03: el paciente no desea adquirir los medicamentos de una receta
    @PostMapping("/recetas/{idReceta}/cancelar")
    public CancelarDespachoResponseDTO cancelarDespacho(@PathVariable Integer idReceta,
                                                         @AuthenticationPrincipal AuthUsuario usuario) {
        return farmaciaService.cancelarDespacho(idReceta, usuario.sucursalScopeOrNull());
    }

    // NUEVO: ajustar / cargar stock de un medicamento en una sucursal.
    // idUsuario ya no viaja en el body: sale del JWT.
    @PostMapping("/inventario/ajustar")
    public AjustarStockResponseDTO ajustarStock(@RequestBody AjustarStockRequestDTO request,
                                                 @AuthenticationPrincipal AuthUsuario usuario) {
        return farmaciaService.ajustarStock(request, usuario.getId(), usuario.sucursalScopeOrNull());
    }
}
