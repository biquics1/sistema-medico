// ============================================================
// ENTIDAD JPA: Usuario -> tabla "usuario"
// Tabla central que representa TANTO al personal interno
// (médico, enfermero, cajero, etc.) COMO a los pacientes
// (rol "Paciente"), diferenciados por el campo rol.
// CU-01 (mantenimiento), CU-02 (registro de pacientes).
// ============================================================
package com.example.demo.modelo;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuario")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // PK autoincremental
    private Integer id;

    @Column(name = "nombre_completo", nullable = false, length = 100)
    private String nombreCompleto; // 10-100 caracteres (RN-CU01-04 / RN-CU02-01)

    @Column(name = "correo_electronico", nullable = false, unique = true, length = 150)
    private String correoElectronico; // Único en el sistema (RN-CU02-04)

    @Column(name = "nombre_usuario", nullable = false, unique = true, length = 9)
    private String nombreUsuario; // 8-9 caracteres, único (RN-CU01-05 / RN-CU02-05)

    @Column(name = "password_hash", nullable = false)
    private String passwordHash; // Contraseña ya hasheada (nunca en texto plano)

    @Column(length = 13)
    private String dpi; // Documento de identificación, 13 dígitos (RN-GLOBAL-001)

    @Column(length = 8)
    private String telefono; // 8 dígitos (RN-CU01-08 / RN-CU02-02)

    @Column(length = 9)
    private String nit; // 8-9 caracteres alfanuméricos (RN-GLOBAL-002)

    @Column(name = "numero_seguro", length = 50)
    private String numeroSeguro; // Número de afiliado a seguro médico (opcional)

    @ManyToOne
    @JoinColumn(name = "rol_id", nullable = false) // FK -> rol del usuario (obligatorio)
    private Rol rol;

    @ManyToOne
    @JoinColumn(name = "sucursal_id") // FK -> sucursal a la que pertenece (opcional en edición)
    private Sucursal sucursal;

    @ManyToOne
    @JoinColumn(name = "especialidad_id") // FK -> especialidad, solo aplica si rol = Médico
    private Especialidad especialidad;

    @Column(nullable = false)
    private Short estado = 1; // 1 = Activo, 0 = Inactivo

    //  bloqueo por intentos fallidos de inicio de sesión
    @Column(name = "intentos_fallidos", nullable = false)
    private Short intentosFallidos = 0; // Contador de logins fallidos consecutivos (máx 5, RN-CU00-03)

    @Column(name = "bloqueado_hasta")
    private LocalDateTime bloqueadoHasta; // Fecha/hora hasta la cual la cuenta permanece bloqueada

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();
}
