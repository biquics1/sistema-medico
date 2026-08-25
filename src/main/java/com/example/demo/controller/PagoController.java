package com.example.demo.controller;

import com.example.demo.dto.PagoCreateDTO;
import com.example.demo.dto.PagoResponseDTO;
import com.example.demo.service.PagoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class PagoController {

    private final PagoService pagoService;

    @PostMapping("/api/payments")
    public PagoResponseDTO pagar(@RequestBody PagoCreateDTO dto) {
        return pagoService.procesarPago(dto);
    }
}