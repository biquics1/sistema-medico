// ============================================================
// REPOSITORY: UsuarioRepository
// Acceso a datos de Usuario (personal interno y pacientes).
// CU-01 (mantenimiento), CU-02 (registro externo), login (CU-00).
// ============================================================
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

    // Login/verificación por DPI (CU-00)
    Optional<Usuario> findByDpi(String dpi);
    // Login por nombre de usuario
    Optional<Usuario> findByNombreUsuario(String nombreUsuario);
    // Valida unicidad de nombre de usuario al crear (RN-CU01-05/RN-CU02-05)
    boolean existsByNombreUsuario(String nombreUsuario);
    // Valida unicidad de correo al crear (RN-CU02-04)
    boolean existsByCorreoElectronico(String correoElectronico);
    // Valida unicidad de nombre de usuario al editar, excluyendo el propio registro
    boolean existsByNombreUsuarioAndIdNot(String nombreUsuario, Integer id);

    // Valida unicidad de correo al editar, excluyendo el propio registro
    boolean existsByCorreoElectronicoAndIdNot(String correoElectronico, Integer id);

    // Busca médicos por rol, sucursal y especialidad (usado al filtrar médicos disponibles, CU-03)
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
