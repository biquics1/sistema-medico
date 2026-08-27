package com.example.demo.controller;

import com.example.demo.dto.*;
import com.example.demo.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Autenticación y registro (CU-00, CU-01, CU-02): verificación de DPI en el
// portal, login (interno y paciente) y alta de pacientes desde el portal público.
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/verify-dpi")
    public ResponseEntity<VerifyDpiResponseDTO> verificarDpi(@Valid @RequestBody VerifyDpiDTO dto) {
        return ResponseEntity.ok(authService.verificarDpi(dto));
    }

    @PostMapping("/auth/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto) {
        return ResponseEntity.ok(authService.login(dto));
    }

    // NUEVO — CU-02
    @PostMapping("/register")
    public ResponseEntity<RegistroPacienteResponseDTO> registrar(@Valid @RequestBody RegistroPacienteDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrarPaciente(dto));
    }
}