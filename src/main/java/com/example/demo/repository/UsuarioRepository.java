package com.example.demo.repository;

import com.example.demo.modelo.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
// JpaSpecificationExecutor: permite combinar en runtime el filtro de
// búsqueda (RN-CU01-01) con el filtro de sede del Administrador de Sede,
// sin tener que duplicar cada método findByXxx en una versión "...AndSucursal_Id".
public interface UsuarioRepository extends JpaRepository<Usuario, Integer>, JpaSpecificationExecutor<Usuario> {

    Optional<Usuario> findByDpi(String dpi);
    Optional<Usuario> findByNombreUsuario(String nombreUsuario);
    boolean existsByNombreUsuario(String nombreUsuario);
    boolean existsByCorreoElectronico(String correoElectronico);
    boolean existsByNombreUsuarioAndIdNot(String nombreUsuario, Integer id);

    boolean existsByCorreoElectronicoAndIdNot(String correoElectronico, Integer id);

    List<Usuario> findByRol_NombreAndSucursal_IdAndEspecialidad_IdAndEstado(String médico, Integer sucursalId, Integer especialidadId, short i);

    // ===== NUEVO: panel Admin General / Admin de Sede — CU-01 + control de citas =====
    // Admin General: médicos de TODAS las sedes.
    List<Usuario> findByRol_NombreAndEstadoOrderByNombreCompletoAsc(String rolNombre, Short estado);
    // Admin de Sede: médicos SOLO de su sucursal.
    List<Usuario> findByRol_NombreAndSucursal_IdAndEstadoOrderByNombreCompletoAsc(String rolNombre, Integer sucursalId, Short estado);

    // Nota: las búsquedas de CU-01 (RN-CU01-01) por ID/nombre/correo/usuario/DPI/rol
    // ahora se resuelven con Specification (ver UsuarioSpecifications) desde
    // UsuarioService.buscar(), porque necesitan combinarse dinámicamente con el
    // filtro de sede del Administrador de Sede.
}
