package com.example.demo.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

// Body para crear un usuario interno (CU-01, FA01). Las validaciones de cada
// campo referencian su regla de negocio correspondiente (RN-CU01-xx).
@Data
public class UsuarioCreateDTO {

    @NotBlank(message = "El campo Nombre es obligatorio.")
    @Size(min = 10, max = 100, message = "El nombre debe contener entre 10 y 100 caracteres.")
    private String nombreCompleto;

    @NotBlank(message = "El campo Correo Electrónico es obligatorio.")
    @Email(message = "El formato del correo electrónico no es válido.")
    private String correoElectronico;

    @NotBlank(message = "El campo Usuario es obligatorio.")
    @Size(min = 8, max = 9, message = "El usuario debe contener entre 8 y 9 caracteres.")
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "El usuario debe contener únicamente caracteres alfanuméricos.")
    private String nombreUsuario;

    @NotBlank(message = "El campo Contraseña es obligatorio.")
    @Size(min = 12, message = "La contraseña debe contener al menos 12 caracteres.")
    private String password;

    @Pattern(regexp = "^\\d{13}$", message = "El DPI debe contener exactamente 13 dígitos.")
    private String dpi;

    @Pattern(regexp = "^\\d{8}$", message = "El teléfono debe contener exactamente 8 dígitos.")
    private String telefono;

    @NotNull(message = "Debe seleccionar un rol para el usuario.")
    private Integer rolId;

    @Size(min = 8, max = 9, message = "El NIT debe contener entre 8 y 9 caracteres.")
    private String nit;

    @Size(min = 5, max = 50, message = "El número de seguro debe contener entre 5 y 50 caracteres.")
    private String numeroSeguro;

    // Ya no es @NotNull a nivel de DTO: la obligatoriedad depende del rol
    // elegido (Administrador General y Paciente NO llevan sucursal; el resto sí).
    // Esa regla se valida en UsuarioService.resolverSucursal().
    private Integer sucursalId;

    private Integer especialidadId;

    private Short estado = 1;
}