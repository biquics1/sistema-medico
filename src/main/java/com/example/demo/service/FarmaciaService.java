package com.example.demo.service;

import com.example.demo.dto.FarmaciaDTOs.*;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.ValidationException;
import com.example.demo.modelo.*;
import com.example.demo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class FarmaciaService {
    private final AuditoriaContexto auditoriaContexto;

    private static final short RECETA_ACTIVA = 1;
    private static final long DIAS_VIGENCIA_RECETA = 7; // RN-CU10-01

    private static final Set<String> TIPOS_AJUSTE_VALIDOS = Set.of("COMPRA", "AJUSTE_POSITIVO", "AJUSTE_NEGATIVO");

    private final RecetaMedicaRepository recetaMedicaRepository;
    private final DetalleRecetaMedicaRepository detalleRecetaMedicaRepository;
    private final MedicamentoRepository medicamentoRepository;
    private final UsuarioRepository usuarioRepository;
    private final SucursalRepository sucursalRepository;
    private final InventarioMedicamentoRepository inventarioMedicamentoRepository;
    private final MovimientoInventarioRepository movimientoInventarioRepository;
    private final DespachoMedicamentoRepository despachoMedicamentoRepository;
    private final DetalleDespachoMedicamentoRepository detalleDespachoMedicamentoRepository;

    // ---------------------------------------------------------------
    // Búsqueda de recetas activas por ID de Receta o ID de Consulta
    // sucursalScope != null -> Administrador de Sede: solo recetas cuya cita
    // de origen pertenece a su sucursal (receta -> consulta -> cita -> sucursal).
    // ---------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<RecetaBusquedaDTO> buscarRecetas(String tipo, String valor, Integer sucursalScope) {
        if (valor == null || valor.isBlank()) {
            throw new ValidationException("Debe ingresar un ID de Receta o ID de Consulta para buscar.");
        }

        Integer id;
        try {
            id = Integer.parseInt(valor.trim());
        } catch (NumberFormatException e) {
            throw new ValidationException("El criterio de búsqueda debe ser un número.");
        }

        List<RecetaMedica> recetas;
        if ("CONSULTA".equalsIgnoreCase(tipo)) {
            recetas = recetaMedicaRepository.findByConsulta_IdAndEstado(id, RECETA_ACTIVA);
        } else {
            recetas = recetaMedicaRepository.findByIdAndEstado(id, RECETA_ACTIVA).map(List::of).orElseGet(List::of);
        }

        recetas = recetas.stream().filter(r -> recetaPerteneceASede(r, sucursalScope)).toList();
        return recetas.stream().map(this::toBusquedaDTO).toList();
    }

    private boolean recetaPerteneceASede(RecetaMedica r, Integer sucursalScope) {
        if (sucursalScope == null) return true;
        var cita = r.getConsulta() != null ? r.getConsulta().getCita() : null;
        return cita != null && cita.getSucursal() != null && sucursalScope.equals(cita.getSucursal().getId());
    }

    // ---------------------------------------------------------------
    // Detalle de la receta para armar el carrito (valida vigencia)
    // Administrador de Sede: idSucursal debe ser su propia sede (o se usa por defecto),
    // y la receta debe pertenecer a esa misma sede.
    // ---------------------------------------------------------------
    @Transactional(readOnly = true)
    public RecetaDetalleDTO obtenerDetalleReceta(Integer idReceta, Integer idSucursal, Integer sucursalScope) {
        if (sucursalScope != null) {
            if (idSucursal != null && !sucursalScope.equals(idSucursal)) {
                throw new ValidationException("No tiene permiso para consultar el inventario de otra sucursal.");
            }
        }
        // CORREGIDO: no se reasigna el parámetro `idSucursal` (eso lo dejaba
        // de ser "effectively final" y rompía la compilación en el lambda de
        // abajo, línea "detalles.stream().map(d -> toItemDTO(d, idSucursal))").
        // En su lugar se resuelve el valor final una sola vez en una variable
        // `final` nueva, que es la que se captura dentro del lambda.
        final Integer idSucursalResuelto = (sucursalScope != null) ? sucursalScope : idSucursal;

        RecetaMedica receta = recetaMedicaRepository.findByIdAndEstado(idReceta, RECETA_ACTIVA)
                .filter(r -> recetaPerteneceASede(r, sucursalScope))
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la receta indicada."));

        long dias = ChronoUnit.DAYS.between(receta.getCreadoEn(), LocalDateTime.now());
        boolean vigente = dias <= DIAS_VIGENCIA_RECETA;

        if (!vigente) {
            throw new ValidationException(String.format(
                    "Receta Vencida. La receta #%d fue emitida hace %d días y ya no es válida para despacho.",
                    receta.getId(), dias));
        }

        List<DetalleRecetaMedica> detalles = detalleRecetaMedicaRepository.findByReceta_Id(idReceta);
        if (detalles.isEmpty()) {
            throw new ValidationException("La receta no tiene medicamentos registrados.");
        }

        List<ItemRecetaDTO> items = detalles.stream()
                .map(d -> toItemDTO(d, idSucursalResuelto))
                .toList();

        BigDecimal montoEstimado = items.stream()
                .map(ItemRecetaDTO::getPrecioUnitario)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return RecetaDetalleDTO.builder()
                .id(receta.getId())
                .consultaId(receta.getConsulta().getId())
                .nombrePaciente(receta.getConsulta().getCita().getPaciente().getNombreCompleto())
                .fechaEmision(receta.getCreadoEn())
                .vigente(true)
                .diasTranscurridos(dias)
                .items(items)
                .montoTotalEstimado(montoEstimado)
                .build();
    }

    // ---------------------------------------------------------------
    // NUEVO: catálogo/búsqueda de medicamentos con stock (venta libre)
    // Administrador de Sede: solo puede consultar el stock de su propia sede.
    // ---------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<MedicamentoCatalogoDTO> buscarMedicamentos(String nombre, Integer idSucursal, Integer sucursalScope) {
        if (sucursalScope != null) {
            if (idSucursal != null && !sucursalScope.equals(idSucursal)) {
                throw new ValidationException("No tiene permiso para consultar el inventario de otra sucursal.");
            }
            idSucursal = sucursalScope;
        }

        List<Medicamento> medicamentos = (nombre == null || nombre.isBlank())
                ? medicamentoRepository.findByEstado((short) 1)
                : medicamentoRepository.findByEstadoAndNombreContainingIgnoreCase((short) 1, nombre.trim());

        Integer idSucursalFinal = idSucursal;
        return medicamentos.stream()
                .map(m -> toMedicamentoCatalogoDTO(m, idSucursalFinal))
                .sorted(Comparator.comparing(MedicamentoCatalogoDTO::getNombre))
                .toList();
    }

    // ---------------------------------------------------------------
    // Pasos 10-13 FB: confirmar el despacho de UNA receta.
    // El cobro físico es externo al sistema: no se crea Pago ni se pide
    // método de pago. Se registra el despacho, sus ítems, el movimiento
    // de inventario y se devuelve el resumen.
    // ---------------------------------------------------------------
    @Transactional
    public ConfirmarDespachoResponseDTO confirmarDespacho(Integer idReceta, ConfirmarDespachoRequestDTO request,
                                                          Integer idFarmaceutico, Integer sucursalScope) {
        auditoriaContexto.aplicar();
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new ValidationException("Debe seleccionar al menos un medicamento a despachar.");
        }
        if (request.getIdSucursal() == null) {
            throw new ValidationException("Debe indicar la sucursal desde la que se despacha.");
        }
        if (sucursalScope != null && !sucursalScope.equals(request.getIdSucursal())) {
            throw new ValidationException("No tiene permiso para despachar medicamentos desde otra sucursal.");
        }
        Sucursal sucursal = sucursalRepository.findById(request.getIdSucursal())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la sucursal indicada."));
        Usuario farmaceutico = usuarioRepository.findById(idFarmaceutico)
                .orElseThrow(() -> new ValidationException("Debe indicar el usuario de farmacia que realiza el despacho."));

        // Paso 5 FB / RN-CU10-01: la receta debe estar activa, ser de la sede y estar vigente
        RecetaMedica receta = recetaMedicaRepository.findByIdAndEstado(idReceta, RECETA_ACTIVA)
                .filter(r -> recetaPerteneceASede(r, sucursalScope))
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la receta #" + idReceta + "."));
        long dias = ChronoUnit.DAYS.between(receta.getCreadoEn(), LocalDateTime.now());
        if (dias > DIAS_VIGENCIA_RECETA) {
            throw new ValidationException(String.format(
                    "Receta Vencida. La receta #%d fue emitida hace %d días y ya no es válida para despacho.",
                    idReceta, dias));
        }

        // Solo se pueden despachar medicamentos que realmente están en la receta
        Set<Integer> medicamentosDeLaReceta = new HashSet<>();
        detalleRecetaMedicaRepository.findByReceta_Id(idReceta)
                .forEach(d -> medicamentosDeLaReceta.add(d.getMedicamento().getId()));
        for (DespachoItemDTO item : request.getItems()) {
            if (item.getMedicamentoId() == null || !medicamentosDeLaReceta.contains(item.getMedicamentoId())) {
                throw new ValidationException("Uno de los medicamentos indicados no pertenece a la receta #" + idReceta + ".");
            }
        }

        DespachoMedicamento despacho = new DespachoMedicamento();
        despacho.setReceta(receta);
        despacho.setFarmaceutico(farmaceutico);
        despacho.setSucursal(sucursal);
        despacho.setEsVentaLibre(false);
        despacho.setPaciente(receta.getConsulta().getCita().getPaciente());
        DespachoMedicamento despachoGuardado = despachoMedicamentoRepository.save(despacho);

        List<ItemDespachadoDTO> itemsDespachados = new ArrayList<>();
        List<String> alertas = new ArrayList<>();
        BigDecimal montoTotal = despacharItems(request.getItems(), despachoGuardado, sucursal, farmaceutico,
                itemsDespachados, alertas);

        if (itemsDespachados.isEmpty()) {
            throw new ValidationException(
                    "No fue posible despachar ningún medicamento. Verifique el inventario de la sucursal.");
        }

        despachoGuardado.setMontoTotal(montoTotal);
        despachoMedicamentoRepository.save(despachoGuardado);

        // Paso 13 FB
        String mensaje = String.format("Despacho registrado exitosamente. %d medicamento(s) despachado(s). Total: Q%.2f.",
                itemsDespachados.size(), montoTotal);

        return ConfirmarDespachoResponseDTO.builder()
                .mensaje(mensaje)
                .despachoId(despachoGuardado.getId())
                .montoTotal(montoTotal)
                .itemsDespachados(itemsDespachados)
                .alertas(alertas)
                .build();
    }

    // Despacha los ítems de la receta y devuelve el monto total.
    private BigDecimal despacharItems(List<DespachoItemDTO> items, DespachoMedicamento despacho, Sucursal sucursal,
                                      Usuario farmaceutico, List<ItemDespachadoDTO> itemsDespachados, List<String> alertas) {
        BigDecimal montoTotal = BigDecimal.ZERO;
        Integer idReceta = despacho.getReceta().getId();

        for (DespachoItemDTO item : items) {
            if (item.getCantidad() == null || item.getCantidad() <= 0) {
                throw new ValidationException("Cada medicamento a despachar debe tener cantidad mayor a 0.");
            }

            Medicamento medicamentoBase = medicamentoRepository.findById(item.getMedicamentoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Medicamento no encontrado en el catálogo."));

            // FA02: sustitución de medicamento
            Medicamento medicamentoAEntregar = medicamentoBase;
            Medicamento medicamentoSustituto = null;
            boolean sustituido = item.isSustituido();
            if (sustituido) {
                if (item.getRazonSustitucion() == null || item.getRazonSustitucion().isBlank()) {
                    throw new ValidationException("Debe ingresar la razón de sustitución.");
                }
                if (item.getMedicamentoSustitutoId() == null) {
                    throw new ValidationException("Debe seleccionar el medicamento alternativo para la sustitución.");
                }
                medicamentoSustituto = medicamentoRepository.findById(item.getMedicamentoSustitutoId())
                        .orElseThrow(() -> new ResourceNotFoundException("Medicamento sustituto no encontrado en el catálogo."));
                medicamentoAEntregar = medicamentoSustituto;
            }

            // FA01: verificar inventario de la sucursal (se continúa con los disponibles)
            Optional<InventarioMedicamento> inventarioOpt = inventarioMedicamentoRepository
                    .findForUpdate(medicamentoAEntregar.getId(), sucursal.getId());

            if (inventarioOpt.isEmpty()) {
                alertas.add(medicamentoAEntregar.getNombre() + ": Sin inventario registrado.");
                continue;
            }

            InventarioMedicamento inventario = inventarioOpt.get();
            if (inventario.getStockActual() < item.getCantidad()) {
                // FA01 paso 2 (RN-CU10-03): stock bajo / insuficiente
                alertas.add(String.format("%s: Stock bajo — disponible: %d (mínimo: %s). No alcanza para la cantidad solicitada (%d).",
                        medicamentoAEntregar.getNombre(), inventario.getStockActual(),
                        medicamentoAEntregar.getStockMinimo() != null ? medicamentoAEntregar.getStockMinimo() : "-",
                        item.getCantidad()));
                continue;
            }

            int stockAnterior = inventario.getStockActual();
            int stockNuevo = stockAnterior - item.getCantidad();
            inventario.setStockActual(stockNuevo);
            inventarioMedicamentoRepository.save(inventario);

            // Movimiento de inventario automático (tipo Despacho)
            MovimientoInventario movimiento = new MovimientoInventario();
            movimiento.setMedicamento(medicamentoAEntregar);
            movimiento.setSucursal(sucursal);
            movimiento.setTipoMovimiento(MovimientoInventario.DESPACHO);
            movimiento.setCantidad(item.getCantidad());
            movimiento.setStockAnterior(stockAnterior);
            movimiento.setStockNuevo(stockNuevo);
            movimiento.setNumeroReferencia("RECETA-" + idReceta);
            movimiento.setMotivo("Despacho automático - Receta #" + idReceta);
            movimiento.setUsuario(farmaceutico);
            movimientoInventarioRepository.save(movimiento);

            BigDecimal precioUnitario = medicamentoAEntregar.getPrecio();
            BigDecimal subtotal = precioUnitario.multiply(BigDecimal.valueOf(item.getCantidad()));
            montoTotal = montoTotal.add(subtotal);

            DetalleDespachoMedicamento detalle = new DetalleDespachoMedicamento();
            detalle.setDespacho(despacho);
            detalle.setMedicamento(medicamentoBase);
            detalle.setCantidad(item.getCantidad());
            detalle.setPrecioUnitario(precioUnitario);
            detalle.setSustituido(sustituido);
            detalle.setMedicamentoSustituto(medicamentoSustituto);
            detalle.setRazonSustitucion(sustituido ? item.getRazonSustitucion() : null);
            detalleDespachoMedicamentoRepository.save(detalle);

            if (sustituido) {
                alertas.add(String.format("Medicamento %s sustituido por %s. El médico tratante será notificado de la sustitución.",
                        medicamentoBase.getNombre(), medicamentoSustituto.getNombre()));
            }

            // FA04: alerta de stock mínimo alcanzado tras el despacho
            if (medicamentoAEntregar.getStockMinimo() != null && stockNuevo <= medicamentoAEntregar.getStockMinimo()) {
                alertas.add(String.format(
                        "ALERTA: El medicamento %s ha alcanzado el nivel de stock mínimo (%d unidades restantes). Se recomienda generar orden de reabastecimiento.",
                        medicamentoAEntregar.getNombre(), stockNuevo));
            }

            itemsDespachados.add(ItemDespachadoDTO.builder()
                    .nombreMedicamento(medicamentoBase.getNombre())
                    .cantidad(item.getCantidad())
                    .precioUnitario(precioUnitario)
                    .subtotal(subtotal)
                    .sustituido(sustituido)
                    .nombreMedicamentoSustituto(medicamentoSustituto != null ? medicamentoSustituto.getNombre() : null)
                    .build());
        }

        return montoTotal;
    }

    // ---------------------------------------------------------------
    // FA03: el paciente no desea adquirir los medicamentos de una receta
    // ---------------------------------------------------------------
    @Transactional(readOnly = true)
    public CancelarDespachoResponseDTO cancelarDespacho(Integer idReceta, Integer sucursalScope) {
        RecetaMedica receta = recetaMedicaRepository.findByIdAndEstado(idReceta, RECETA_ACTIVA)
                .filter(r -> recetaPerteneceASede(r, sucursalScope))
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la receta indicada."));

        String nombrePaciente = receta.getConsulta().getCita().getPaciente().getNombreCompleto();
        String mensaje = String.format(
                "Se ha registrado que el paciente %s no adquirió los medicamentos recetados en farmacia interna. Receta: %d.",
                nombrePaciente, receta.getId());

        return CancelarDespachoResponseDTO.builder().mensaje(mensaje).build();
    }

    // ---------------------------------------------------------------
    // NUEVO: alta de medicamentos al catálogo (Administrador), CU-15 (RN-CU15-01/02)
    // ---------------------------------------------------------------
    @Transactional
    public MedicamentoCatalogoDTO crearMedicamento(CrearMedicamentoRequestDTO request) {
        auditoriaContexto.aplicar();
        if (request.getNombre() == null || request.getNombre().isBlank()) {
            throw new ValidationException("El nombre es obligatorio.");
        }
        if (request.getDescripcion() == null || request.getDescripcion().isBlank()) {
            throw new ValidationException("La descripción es obligatoria.");
        }
        if (request.getPrecio() == null || request.getPrecio().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("El precio debe ser mayor a 0.");
        }
        if (request.getUnidad() == null || request.getUnidad().isBlank()) {
            throw new ValidationException("La unidad es obligatoria.");
        }
        if (request.getStockMinimo() != null && request.getStockMinimo() < 0) {
            throw new ValidationException("El stock mínimo no puede ser negativo.");
        }
        if (medicamentoRepository.existsByNombreIgnoreCaseAndEstado(request.getNombre().trim(), (short) 1)) {
            throw new ValidationException("Ya existe un registro con el nombre " + request.getNombre() + ".");
        }

        Medicamento medicamento = new Medicamento();
        medicamento.setNombre(request.getNombre().trim());
        medicamento.setDescripcion(request.getDescripcion().trim());
        medicamento.setPrecio(request.getPrecio());
        medicamento.setUnidad(request.getUnidad().trim());
        medicamento.setEsControlado(request.isEsControlado());
        medicamento.setStockMinimo(request.getStockMinimo());
        medicamento.setEstado((short) 1);
        Medicamento guardado = medicamentoRepository.save(medicamento);

        return toMedicamentoCatalogoDTO(guardado, null);
    }

    // ---------------------------------------------------------------
    // NUEVO: ajuste/carga de stock (Administrador), mismo patrón que CU-13 Bitácora
    // ---------------------------------------------------------------
    @Transactional
    public AjustarStockResponseDTO ajustarStock(AjustarStockRequestDTO request, Integer idUsuario, Integer sucursalScope) {
        auditoriaContexto.aplicar();
        if (request.getMedicamentoId() == null) {
            throw new ValidationException("Debe seleccionar un medicamento.");
        }
        if (request.getSucursalId() == null) {
            throw new ValidationException("Debe seleccionar una sucursal.");
        }
        if (sucursalScope != null && !sucursalScope.equals(request.getSucursalId())) {
            throw new ValidationException("No tiene permiso para ajustar inventario de otra sucursal.");
        }
        if (request.getCantidad() == null || request.getCantidad() <= 0) {
            throw new ValidationException("La cantidad debe ser un número entero positivo.");
        }
        if (request.getTipoMovimiento() == null || !TIPOS_AJUSTE_VALIDOS.contains(request.getTipoMovimiento())) {
            throw new ValidationException("Debe seleccionar el tipo de movimiento.");
        }
        if ("COMPRA".equals(request.getTipoMovimiento())) {
            if (request.getCostoUnitario() == null || request.getCostoUnitario().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidationException("El costo unitario es obligatorio para compras y debe ser mayor a 0.");
            }
        } else if (request.getMotivo() == null || request.getMotivo().trim().length() < 10) {
            throw new ValidationException("El motivo debe contener entre 10 y 500 caracteres.");
        }

        Medicamento medicamento = medicamentoRepository.findById(request.getMedicamentoId())
                .orElseThrow(() -> new ResourceNotFoundException("Medicamento no encontrado en el catálogo."));
        Sucursal sucursal = sucursalRepository.findById(request.getSucursalId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la sucursal indicada."));

        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ValidationException("Debe indicar el usuario que registra el movimiento."));

        InventarioMedicamento inventario = inventarioMedicamentoRepository
                .findForUpdate(medicamento.getId(), sucursal.getId())
                .orElseGet(() -> {
                    InventarioMedicamento nuevo = new InventarioMedicamento();
                    nuevo.setMedicamento(medicamento);
                    nuevo.setSucursal(sucursal);
                    nuevo.setStockActual(0);
                    return nuevo;
                });

        int stockAnterior = inventario.getStockActual();
        int stockNuevo;
        short tipo;

        if ("AJUSTE_NEGATIVO".equals(request.getTipoMovimiento())) {
            if (request.getCantidad() > stockAnterior) {
                throw new ValidationException(String.format(
                        "Stock insuficiente. El stock actual es %d unidades.", stockAnterior));
            }
            stockNuevo = stockAnterior - request.getCantidad();
            tipo = MovimientoInventario.AJUSTE_NEGATIVO;
        } else {
            stockNuevo = stockAnterior + request.getCantidad();
            tipo = "COMPRA".equals(request.getTipoMovimiento())
                    ? MovimientoInventario.COMPRA
                    : MovimientoInventario.AJUSTE_POSITIVO;
        }

        inventario.setStockActual(stockNuevo);
        inventarioMedicamentoRepository.save(inventario);

        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setMedicamento(medicamento);
        movimiento.setSucursal(sucursal);
        movimiento.setTipoMovimiento(tipo);
        movimiento.setCantidad(request.getCantidad());
        movimiento.setStockAnterior(stockAnterior);
        movimiento.setStockNuevo(stockNuevo);
        movimiento.setCostoUnitario("COMPRA".equals(request.getTipoMovimiento()) ? request.getCostoUnitario() : null);
        movimiento.setNumeroReferencia(request.getTipoMovimiento());
        movimiento.setMotivo("COMPRA".equals(request.getTipoMovimiento())
                ? (request.getMotivo() != null ? request.getMotivo() : "Entrada por compra")
                : request.getMotivo());
        movimiento.setUsuario(usuario);
        movimientoInventarioRepository.save(movimiento);

        String mensaje = String.format(
                "Movimiento registrado exitosamente. Medicamento: %s. Tipo: %s. Cantidad: %d. Stock actualizado: %d.",
                medicamento.getNombre(), request.getTipoMovimiento(), request.getCantidad(), stockNuevo);

        return AjustarStockResponseDTO.builder()
                .mensaje(mensaje)
                .stockAnterior(stockAnterior)
                .stockNuevo(stockNuevo)
                .build();
    }

    // ---------------------------------------------------------------
    private RecetaBusquedaDTO toBusquedaDTO(RecetaMedica r) {
        long dias = ChronoUnit.DAYS.between(r.getCreadoEn(), LocalDateTime.now());
        return RecetaBusquedaDTO.builder()
                .id(r.getId())
                .consultaId(r.getConsulta().getId())
                .fechaEmision(r.getCreadoEn())
                .vigente(dias <= DIAS_VIGENCIA_RECETA)
                .diasTranscurridos(dias)
                .notas(r.getNotas())
                .build();
    }

    private ItemRecetaDTO toItemDTO(DetalleRecetaMedica d, Integer idSucursal) {
        Medicamento m = d.getMedicamento();
        Integer stockDisponible = null;
        boolean stockBajo = false;
        if (idSucursal != null) {
            Optional<InventarioMedicamento> inv = inventarioMedicamentoRepository
                    .findByMedicamentoIdAndSucursalId(m.getId(), idSucursal);
            if (inv.isPresent()) {
                stockDisponible = inv.get().getStockActual();
                stockBajo = m.getStockMinimo() != null && stockDisponible <= m.getStockMinimo();
            }
        }
        return ItemRecetaDTO.builder()
                .medicamentoId(m.getId())
                .nombreMedicamento(m.getNombre())
                .dosis(d.getDosis())
                .frecuencia(d.getFrecuencia())
                .duracion(d.getDuracion())
                .indicaciones(d.getIndicaciones())
                .precioUnitario(m.getPrecio())
                .stockDisponible(stockDisponible)
                .stockMinimo(m.getStockMinimo())
                .stockBajo(stockBajo)
                .build();
    }

    private MedicamentoCatalogoDTO toMedicamentoCatalogoDTO(Medicamento m, Integer idSucursal) {
        Integer stockDisponible = null;
        boolean stockBajo = false;
        if (idSucursal != null) {
            Optional<InventarioMedicamento> inv = inventarioMedicamentoRepository
                    .findByMedicamentoIdAndSucursalId(m.getId(), idSucursal);
            if (inv.isPresent()) {
                stockDisponible = inv.get().getStockActual();
                stockBajo = m.getStockMinimo() != null && stockDisponible <= m.getStockMinimo();
            }
        }
        return MedicamentoCatalogoDTO.builder()
                .id(m.getId())
                .nombre(m.getNombre())
                .descripcion(m.getDescripcion())
                .precio(m.getPrecio())
                .unidad(m.getUnidad())
                .esControlado(m.isEsControlado())
                .stockDisponible(stockDisponible)
                .stockMinimo(m.getStockMinimo())
                .stockBajo(stockBajo)
                .build();
    }
}