package com.example.demo.service;

import com.example.demo.dto.PagoCreateDTO;
import com.example.demo.dto.PagoResponseDTO;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.ValidationException;
import com.example.demo.modelo.*;
import com.example.demo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class PagoService {
    private final AuditoriaContexto auditoriaContexto;

    private final PagoRepository pagoRepository;
    private final CitaRepository citaRepository;
    private final EstadoCitaRepository estadoCitaRepository;
    private final EmailService emailService;

    private static final Pattern VENC_PATTERN = Pattern.compile("^(0[1-9]|1[0-2])/\\d{2}$");

    private static final String ESTADO_CONFIRMADA = "Confirmada";

    @Transactional
    public PagoResponseDTO procesarPago(PagoCreateDTO dto) {
        auditoriaContexto.aplicar();

        if (dto.getIdempotencyKey() != null) {
            var existente = pagoRepository.findByIdempotencyKey(dto.getIdempotencyKey());
            if (existente.isPresent()) {
                Pago p = existente.get();
                return new PagoResponseDTO(p.getCita().getId(), p.getNumeroTransaccion(),
                        p.getMonto(), p.getCita().getEstadoCita().getNombre(),
                        "¡Pago realizado exitosamente! Número de transacción: " + p.getNumeroTransaccion());
            }
        }

        Cita cita = citaRepository.findById(dto.getCitaId())
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada."));

        if (!"Pendiente de pago".equals(cita.getEstadoCita().getNombre())) {
            throw new ValidationException("Esta cita ya no está pendiente de pago.");
        }
        // Ventana de 5 min de la SESIÓN de pago en línea (CU-04). Si vence, se
        // cancela la cita aquí mismo (respaldo por si el frontend no alcanzó a
        // llamar /cancelar-expirada antes de este intento de pago).
        if (cita.getSesionPagoExpiraEn() != null && cita.getSesionPagoExpiraEn().isBefore(LocalDateTime.now())) {
            EstadoCita cancelada = estadoCitaRepository.findByNombre(EstadoCita.CANCELADA)
                    .orElseThrow(() -> new ResourceNotFoundException("Estado 'Cancelada' no configurado."));
            cita.setEstadoCita(cancelada);
            citaRepository.save(cita);
            throw new ValidationException(
                    "El tiempo para confirmar su cita ha expirado. El horario seleccionado ha sido liberado. " +
                            "Por favor, seleccione un nuevo horario.");
        }

        validarTarjeta(dto);

        if ("4000000000000002".equals(dto.getNumeroTarjeta())) {
            throw new ValidationException(
                    "La transacción con tarjeta fue rechazada por el banco. " +
                            "Por favor, verifique los datos de su tarjeta o intente con una tarjeta diferente.");
        }

        String numeroTransaccion = "TXN-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();

        Pago pago = new Pago();
        pago.setCita(cita);
        pago.setPaciente(cita.getPaciente());
        pago.setNumeroTransaccion(numeroTransaccion);
        pago.setMonto(cita.getMonto());
        pago.setMetodoPago("TARJETA");
        pago.setUltimos4Tarjeta(dto.getNumeroTarjeta().substring(dto.getNumeroTarjeta().length() - 4));
        pago.setNombreTitular(dto.getNombreTitular().toUpperCase());
        pago.setEstado("APROBADO");
        pago.setIdempotencyKey(dto.getIdempotencyKey());
        pagoRepository.save(pago);

        EstadoCita confirmada = estadoCitaRepository.findByNombre(ESTADO_CONFIRMADA)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Estado '" + ESTADO_CONFIRMADA + "' no configurado."));
        cita.setEstadoCita(confirmada);
        cita.setExpiraEn(null);
        cita.setSesionPagoExpiraEn(null);
        citaRepository.save(cita);

        emailService.enviarComprobantePago(cita, pago);

        return new PagoResponseDTO(cita.getId(), numeroTransaccion, pago.getMonto(),
                ESTADO_CONFIRMADA, "¡Pago realizado exitosamente! Número de transacción: " +
                numeroTransaccion + ". Su cita ha sido confirmada.");
    }

    private void validarTarjeta(PagoCreateDTO dto) {
        String numero = dto.getNumeroTarjeta() == null ? "" : dto.getNumeroTarjeta().replaceAll("\\s", "");
        if (numero.length() < 13 || numero.length() > 19 || !numero.matches("\\d+") || !esLuhnValido(numero)) {
            throw new ValidationException(
                    "El número de tarjeta debe contener entre 13 y 19 dígitos y ser válido.");
        }
        if (dto.getNombreTitular() == null ||
                dto.getNombreTitular().length() < 5 ||
                dto.getNombreTitular().length() > 100 ||
                !dto.getNombreTitular().matches("[A-Za-zÁÉÍÓÚÑáéíóúñ ]+")) {
            throw new ValidationException(
                    "El nombre del titular debe contener entre 5 y 100 caracteres alfabéticos sin especiales.");
        }
        if (dto.getVencimiento() == null || !VENC_PATTERN.matcher(dto.getVencimiento()).matches()) {
            throw new ValidationException(
                    "La fecha de vencimiento debe estar en formato MM/AA y la tarjeta no debe estar vencida.");
        }
        YearMonth vencimiento = YearMonth.parse("20" + dto.getVencimiento().substring(3) + "-" +
                        dto.getVencimiento().substring(0, 2),
                DateTimeFormatter.ofPattern("yyyy-MM"));
        if (vencimiento.isBefore(YearMonth.now())) {
            throw new ValidationException(
                    "La fecha de vencimiento debe estar en formato MM/AA y la tarjeta no debe estar vencida.");
        }
        if (dto.getCvv() == null || !dto.getCvv().matches("\\d{3,4}")) {
            throw new ValidationException("El CVV debe contener 3 ó 4 dígitos numéricos.");
        }
    }

    private boolean esLuhnValido(String numero) {
        int suma = 0;
        boolean alternar = false;
        for (int i = numero.length() - 1; i >= 0; i--) {
            int n = Character.getNumericValue(numero.charAt(i));
            if (alternar) {
                n *= 2;
                if (n > 9) n -= 9;
            }
            suma += n;
            alternar = !alternar;
        }
        return suma % 10 == 0;
    }
}