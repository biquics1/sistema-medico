package com.example.demo.service;

import com.example.demo.dto.CajaDTOs.*;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.ValidationException;
import com.example.demo.modelo.*;
import com.example.demo.repository.CitaRepository;
import com.example.demo.repository.DetalleOrdenLaboratorioRepository;
import com.example.demo.repository.EstadoCitaRepository;
import com.example.demo.repository.OrdenLaboratorioRepository;
import com.example.demo.repository.PagoRepository;
import com.example.demo.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CajaService {
    private final AuditoriaContexto auditoriaContexto;

    private static final Pattern DPI_PATTERN = Pattern.compile("^\\d{13}$");
    private static final Pattern CUATRO_DIGITOS = Pattern.compile("^\\d{4}$");
    private static final Set<String> METODOS_VALIDOS = Set.of("EFECTIVO", "VISA", "MASTERCARD", "DEBITO");
    private static final String TARJETA_RECHAZO_SIMULADA = "0002"; // igual convención que CU-04

    private final CitaRepository citaRepository;
    private final UsuarioRepository usuarioRepository;
    private final EstadoCitaRepository estadoCitaRepository;
    private final PagoRepository pagoRepository;
    private final OrdenLaboratorioRepository ordenLaboratorioRepository;
    private final DetalleOrdenLaboratorioRepository detalleOrdenLaboratorioRepository;

    // ---------------------------------------------------------------
    // Paso 1 FB / RN-CU06-01: buscar cita pendiente de pago
    // sucursalScope != null -> Administrador de Sede o Cajero: solo ve citas
    // de su propia sucursal (AuthUsuario.sucursalScopeOrNull()). null ->
    // Administrador General: sin restricción.
    // ---------------------------------------------------------------
    @Transactional(readOnly = true)
    public BusquedaCobroResultadoDTO buscarCitaParaCobro(String tipo, String valor, Integer sucursalScope) {
        if (valor == null || valor.isBlank()) {
            throw new ValidationException("Debe ingresar un número de cita o DPI para buscar.");
        }

        if ("CITA".equalsIgnoreCase(tipo)) {
            Integer idCita;
            try {
                idCita = Integer.parseInt(valor.trim());
            } catch (NumberFormatException e) {
                return sinResultados();
            }
            return citaRepository.findByIdAndEstadoCita_Nombre(idCita, EstadoCita.PENDIENTE_PAGO)
                    .filter(c -> perteneceASede(c, sucursalScope))
                    .map(c -> BusquedaCobroResultadoDTO.builder()
                            .encontrada(true).cita(toDTO(c)).mensaje(null).build())
                    .orElseGet(this::sinResultados);
        }

        if (!DPI_PATTERN.matcher(valor.trim()).matches()) {
            throw new ValidationException("El DPI debe contener exactamente 13 dígitos numéricos.");
        }
        List<Cita> citas = citaRepository.findByPaciente_DpiAndEstadoCita_Nombre(valor.trim(), EstadoCita.PENDIENTE_PAGO)
                .stream().filter(c -> perteneceASede(c, sucursalScope)).toList();
        if (citas.isEmpty()) {
            return sinResultados();
        }
        // CORREGIDO: sin ORDER BY, si el paciente tenía más de una cita
        // "Pendiente de pago" (ej. la de hoy + una futura), el orden de
        // citas.get(0) era indeterminado y podía cobrarse la cita
        // equivocada. Se prioriza la cita de hoy si existe.
        return BusquedaCobroResultadoDTO.builder()
                .encontrada(true).cita(toDTO(elegirCitaMasRelevante(citas))).mensaje(null).build();
    }

    private Cita elegirCitaMasRelevante(List<Cita> citas) {
        var hoy = java.time.LocalDate.now();
        return citas.stream()
                .filter(c -> c.getFechaHora().toLocalDate().equals(hoy))
                .findFirst()
                .orElseGet(() -> citas.stream()
                        .min(java.util.Comparator.comparing(Cita::getFechaHora))
                        .orElse(citas.get(0)));
    }

    private boolean perteneceASede(Cita c, Integer sucursalScope) {
        return sucursalScope == null || sucursalScope.equals(c.getSucursal().getId());
    }

    private boolean ordenPerteneceASede(OrdenLaboratorio o, Integer sucursalScope) {
        if (sucursalScope == null) return true;
        Usuario medico = o.getMedico();
        return medico != null && medico.getSucursal() != null && sucursalScope.equals(medico.getSucursal().getId());
    }

    // FA02: "No hay citas pendientes de pago bajo los parámetros indicados."
    private BusquedaCobroResultadoDTO sinResultados() {
        return BusquedaCobroResultadoDTO.builder()
                .encontrada(false)
                .cita(null)
                .mensaje("No hay citas pendientes de pago bajo los parámetros indicados.")
                .build();
    }

    // ---------------------------------------------------------------
    // Paso 6-9 FB: procesar el cobro (FA01, FA04)
    // ---------------------------------------------------------------
    @Transactional
    public ComprobantePagoDTO cobrar(Integer idCita, CobrarRequestDTO request, Integer idCajero, Integer sucursalScope) {
        auditoriaContexto.aplicar();
        Cita cita = citaRepository.findByIdAndEstadoCita_Nombre(idCita, EstadoCita.PENDIENTE_PAGO)
                .filter(c -> perteneceASede(c, sucursalScope))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No hay citas pendientes de pago bajo los parámetros indicados."));

        String metodo = request.getMetodoPago() == null ? "" : request.getMetodoPago().trim().toUpperCase();
        if (!METODOS_VALIDOS.contains(metodo)) {
            throw new ValidationException("El método de pago seleccionado no está disponible. "
                    + "Los métodos aceptados son: efectivo (Quetzales), tarjeta de crédito (Visa/Mastercard) o tarjeta de débito.");
        }

        BigDecimal montoTotal = cita.getMonto();
        BigDecimal montoRecibido = null;
        BigDecimal cambio = BigDecimal.ZERO;
        String ultimosCuatro = null;

        if ("EFECTIVO".equals(metodo)) {
            montoRecibido = request.getMontoRecibido();
            if (montoRecibido == null) {
                throw new ValidationException("Debe ingresar el monto recibido.");
            }
            if (montoRecibido.compareTo(montoTotal) < 0) {
                throw new ValidationException(String.format(
                        "El monto recibido (Q%.2f) es menor al monto a cobrar (Q%.2f)",
                        montoRecibido, montoTotal));
            }
            cambio = montoRecibido.subtract(montoTotal);

        } else {
            // FA01: pago con tarjeta
            ultimosCuatro = request.getUltimosCuatroDigitos();
            if (ultimosCuatro == null || !CUATRO_DIGITOS.matcher(ultimosCuatro).matches()) {
                throw new ValidationException("Ingrese los últimos 4 dígitos de la tarjeta.");
            }
            // FA04: simulación de rechazo (misma convención que CU-04: termina en 0002)
            if (TARJETA_RECHAZO_SIMULADA.equals(ultimosCuatro)) {
                throw new ValidationException(
                        "La transacción con tarjeta fue rechazada por el banco. Solicite al paciente otro método de pago.");
            }
            montoRecibido = montoTotal;
        }

        if (request.getUuidIdempotencia() != null && !request.getUuidIdempotencia().isBlank()
                && pagoRepository.existsByIdempotencyKey(request.getUuidIdempotencia())) {
            throw new ValidationException("Este cobro ya fue procesado anteriormente.");
        }

        Usuario cajero = null;
        if (idCajero != null) {
            cajero = usuarioRepository.findById(idCajero).orElse(null);
        }

        Pago pago = new Pago();
        pago.setCita(cita);
        pago.setPaciente(cita.getPaciente());
        pago.setCajero(cajero);
        pago.setNumeroTransaccion("TXN-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase());
        pago.setMonto(montoTotal);
        pago.setMontoRecibido(montoRecibido);
        pago.setCambioDevuelto(cambio);
        pago.setMetodoPago(metodo);
        pago.setUltimos4Tarjeta(ultimosCuatro);
        pago.setEstado("APROBADO");
        pago.setIdempotencyKey(request.getUuidIdempotencia());
        pagoRepository.save(pago);

        EstadoCita confirmada = estadoCitaRepository.findByNombre(EstadoCita.CONFIRMADA)
                .orElseThrow(() -> new ResourceNotFoundException("Catálogo estado_cita incompleto: falta 'Confirmada'."));
        cita.setEstadoCita(confirmada);
        citaRepository.save(cita);

        String nombrePaciente = cita.getPaciente().getNombreCompleto();
        String mensaje = String.format(
                "¡Pago registrado exitosamente! Paciente: %s. La cita ha sido actualizada a estado Confirmada.",
                nombrePaciente);

        return ComprobantePagoDTO.builder()
                .mensaje(mensaje)
                .numeroTransaccion(pago.getNumeroTransaccion())
                .nombrePaciente(nombrePaciente)
                .montoTotal(montoTotal)
                .montoRecibido(montoRecibido)
                .cambioDevuelto(cambio)
                .metodoPago(metodo)
                .sucursal(cita.getSucursal().getNombre())
                .detalleServicio("Consulta médica - " + cita.getEspecialidad().getNombre())
                .fechaPago(pago.getCreadoEn())
                .build();
    }

    // ---------------------------------------------------------------
    // Cobro de Laboratorio en Caja (precondición RN-CU09-01 de CU-09)
    // Búsqueda por DPI o por número de orden — solo órdenes "Pendiente" (0)
    // ---------------------------------------------------------------
    @Transactional(readOnly = true)
    public BusquedaCobroLabResultadoDTO buscarOrdenLabParaCobro(String tipo, String valor, Integer sucursalScope) {
        if (valor == null || valor.isBlank()) {
            throw new ValidationException("Debe ingresar un número de orden o DPI para buscar.");
        }

        List<OrdenLaboratorio> ordenes;
        if ("ORDEN".equalsIgnoreCase(tipo)) {
            Integer idOrden;
            try {
                idOrden = Integer.parseInt(valor.trim());
            } catch (NumberFormatException e) {
                ordenes = List.of();
                return sinResultadosLab();
            }
            ordenes = ordenLaboratorioRepository.findByIdAndEstado(idOrden, OrdenLaboratorio.PENDIENTE)
                    .map(List::of).orElseGet(List::of);
        } else {
            if (!DPI_PATTERN.matcher(valor.trim()).matches()) {
                throw new ValidationException("El DPI debe contener exactamente 13 dígitos numéricos.");
            }
            ordenes = ordenLaboratorioRepository.findByPaciente_DpiAndEstado(valor.trim(), OrdenLaboratorio.PENDIENTE);
        }

        ordenes = ordenes.stream().filter(o -> ordenPerteneceASede(o, sucursalScope)).toList();
        if (ordenes.isEmpty()) {
            return sinResultadosLab();
        }

        List<OrdenLabCobroDTO> dtos = ordenes.stream().map(this::toOrdenLabDTO).toList();
        return BusquedaCobroLabResultadoDTO.builder().encontrada(true).ordenes(dtos).mensaje(null).build();
    }

    private BusquedaCobroLabResultadoDTO sinResultadosLab() {
        return BusquedaCobroLabResultadoDTO.builder()
                .encontrada(false)
                .ordenes(List.of())
                .mensaje("No se encontraron órdenes de laboratorio pendientes de pago. Verifique el DPI o número de orden e intente de nuevo.")
                .build();
    }

    // Pasos 6-9 FB de "Cobro de Laboratorio en Caja"
    @Transactional
    public ComprobantePagoDTO cobrarLaboratorio(Integer idOrden, CobrarRequestDTO request, Integer idCajero, Integer sucursalScope) {
        auditoriaContexto.aplicar();
        OrdenLaboratorio orden = ordenLaboratorioRepository.findByIdAndEstado(idOrden, OrdenLaboratorio.PENDIENTE)
                .filter(o -> ordenPerteneceASede(o, sucursalScope))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontraron órdenes de laboratorio pendientes de pago bajo los parámetros indicados."));

        String metodo = request.getMetodoPago() == null ? "" : request.getMetodoPago().trim().toUpperCase();
        if (!METODOS_VALIDOS.contains(metodo)) {
            throw new ValidationException("El método de pago seleccionado no está disponible. "
                    + "Los métodos aceptados son: efectivo (Quetzales), tarjeta de crédito (Visa/Mastercard) o tarjeta de débito.");
        }

        BigDecimal montoTotal = orden.getMontoTotal();
        BigDecimal montoRecibido;
        BigDecimal cambio = BigDecimal.ZERO;
        String ultimosCuatro = null;

        if ("EFECTIVO".equals(metodo)) {

            montoRecibido = request.getMontoRecibido();
            if (montoRecibido == null) {
                throw new ValidationException("Debe ingresar el monto recibido.");
            }
            if (montoRecibido.compareTo(montoTotal) < 0) {
                throw new ValidationException(String.format(
                        "El monto recibido (Q%.2f) es menor al monto a cobrar (Q%.2f)", montoRecibido, montoTotal));
            }
            cambio = montoRecibido.subtract(montoTotal);
        } else {
            ultimosCuatro = request.getUltimosCuatroDigitos();
            if (ultimosCuatro == null || !CUATRO_DIGITOS.matcher(ultimosCuatro).matches()) {
                throw new ValidationException("Ingrese los últimos 4 dígitos de la tarjeta.");
            }
            if (TARJETA_RECHAZO_SIMULADA.equals(ultimosCuatro)) {
                throw new ValidationException(
                        "La transacción con tarjeta fue rechazada por el banco. Solicite al paciente otro método de pago.");
            }
            montoRecibido = montoTotal;
        }

        if (request.getUuidIdempotencia() != null && !request.getUuidIdempotencia().isBlank()
                && pagoRepository.existsByIdempotencyKey(request.getUuidIdempotencia())) {
            throw new ValidationException("Este cobro ya fue procesado anteriormente.");
        }

        Usuario cajero = null;
        if (idCajero != null) {
            cajero = usuarioRepository.findById(idCajero).orElse(null);
        }

        Pago pago = new Pago();
        pago.setPaciente(orden.getPaciente());
        pago.setCajero(cajero);
        pago.setNumeroTransaccion("TXN-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase());
        pago.setMonto(montoTotal);
        pago.setMontoRecibido(montoRecibido);
        pago.setCambioDevuelto(cambio);
        pago.setMetodoPago(metodo);
        pago.setUltimos4Tarjeta(ultimosCuatro);
        pago.setEstado("APROBADO");
        pago.setIdempotencyKey(request.getUuidIdempotencia());
        Pago pagoGuardado = pagoRepository.save(pago);

        orden.setEstado(OrdenLaboratorio.EN_PROCESO);
        orden.setPago(pagoGuardado);
        ordenLaboratorioRepository.save(orden);

        String nombrePaciente = orden.getPaciente().getNombreCompleto();
        String mensaje = String.format(
                "¡Pago de laboratorio registrado exitosamente! Paciente: %s. La orden ha sido actualizada a estado 'En proceso'.",
                nombrePaciente);

        return ComprobantePagoDTO.builder()
                .mensaje(mensaje)
                .numeroTransaccion(pagoGuardado.getNumeroTransaccion())
                .nombrePaciente(nombrePaciente)
                .montoTotal(montoTotal)
                .montoRecibido(montoRecibido)
                .cambioDevuelto(cambio)
                .metodoPago(metodo)
                .sucursal("Laboratorio")
                .detalleServicio("Orden de laboratorio #" + orden.getId())
                .fechaPago(pagoGuardado.getCreadoEn())
                .build();
    }

    private OrdenLabCobroDTO toOrdenLabDTO(OrdenLaboratorio o) {
        int cantidad = detalleOrdenLaboratorioRepository.findByOrden_Id(o.getId()).size();
        return OrdenLabCobroDTO.builder()
                .id(o.getId())
                .nombrePaciente(o.getPaciente().getNombreCompleto())
                .dpiPaciente(o.getPaciente().getDpi())
                .cantidadExamenes(cantidad)
                .fechaCreacion(o.getCreadoEn())
                .montoTotal(o.getMontoTotal())
                .build();
    }

    private CitaCobroDTO toDTO(Cita c) {
        return CitaCobroDTO.builder()
                .id(c.getId())
                .nombrePaciente(c.getPaciente().getNombreCompleto())
                .dpiPaciente(c.getPaciente().getDpi())
                .especialidad(c.getEspecialidad().getNombre())
                .sucursal(c.getSucursal().getNombre())
                .nombreMedico(c.getMedico().getNombreCompleto())
                .fechaHora(c.getFechaHora())
                .monto(c.getMonto())
                .build();
    }
}