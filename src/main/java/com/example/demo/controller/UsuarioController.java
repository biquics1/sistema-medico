package com.example.demo.controller;

import com.example.demo.dto.PageResponseDTO;
import com.example.demo.dto.UsuarioCreateDTO;
import com.example.demo.dto.UsuarioResponseDTO;
import com.example.demo.dto.UsuarioUpdateDTO;
import com.example.demo.security.AuthUsuario;
import com.example.demo.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Mantenimiento de Usuarios internos (CU-01): listado, búsqueda con filtros y
// paginación, obtención por ID, creación, edición y eliminación.
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    // Administrador de Sede (rol "Administrador" + sucursal_id): solo ve/gestiona
    // usuarios de su propia sede. Administrador General (sucursal_id null): sin restricción.
    // Ver AuthUsuario.sucursalScopeOrNull().

    @GetMapping
    public ResponseEntity<List<UsuarioResponseDTO>> listar(@AuthenticationPrincipal AuthUsuario admin) {
        return ResponseEntity.ok(usuarioService.listar(admin.sucursalScopeOrNull()));
    }

    @GetMapping("/buscar")
    public ResponseEntity<PageResponseDTO<UsuarioResponseDTO>> buscar(
            @RequestParam(required = false) String campo,
            @RequestParam(required = false) String valor,
            @RequestParam(required = false) Integer pagina,
            @RequestParam(required = false) Integer tamano,
            @AuthenticationPrincipal AuthUsuario admin) {
        return ResponseEntity.ok(usuarioService.buscar(campo, valor, pagina, tamano, admin.sucursalScopeOrNull()));
    }

    // necesario para precargar el formulario de "Editar Usuario"
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> obtenerPorId(@PathVariable Integer id,
                                                           @AuthenticationPrincipal AuthUsuario admin) {
        return ResponseEntity.ok(usuarioService.obtenerPorId(id, admin.sucursalScopeOrNull()));
    }

    @PostMapping
    public ResponseEntity<UsuarioResponseDTO> crear(@Valid @RequestBody UsuarioCreateDTO dto,
                                                    @AuthenticationPrincipal AuthUsuario admin) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.crear(dto, admin.sucursalScopeOrNull()));
    }

    // NUEVO — CU-01 FA04 (Editar usuario)
    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> actualizar(@PathVariable Integer id, @Valid @RequestBody UsuarioUpdateDTO dto,
                                                         @AuthenticationPrincipal AuthUsuario admin) {
        return ResponseEntity.ok(usuarioService.actualizar(id, dto, admin.sucursalScopeOrNull()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id, @AuthenticationPrincipal AuthUsuario admin) {
        usuarioService.eliminar(id, admin.sucursalScopeOrNull());
        return ResponseEntity.noContent().build();
    }
}
