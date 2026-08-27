// ============================================================
// ENTIDAD JPA: MovimientoInventario -> tabla "movimiento_inventario"
// Bitácora auditable de TODOS los movimientos de stock de
// medicamentos (compras, ventas, ajustes, despachos). CU-13.
// ============================================================
package com.example.demo.modelo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "movimiento_inventario")
@Getter
@Setter
@NoArgsConstructor
public class MovimientoInventario {

    // 0=Compra, 1=Devolución, 2=Venta, 3=Reclamo, 4=Ajuste+, 5=Ajuste-, 6=Despacho (automático)
    public static final short COMPRA = 0;
    public static final short DEVOLUCION = 1;
    public static final short VENTA = 2;
    public static final short RECLAMO = 3;
    public static final short AJUSTE_POSITIVO = 4;
    public static final short AJUSTE_NEGATIVO = 5;
    public static final short DESPACHO = 6; // Generado automáticamente por el módulo de despacho, no manual

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // PK autoincremental
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicamento_id", nullable = false) // FK -> medicamento afectado
    private Medicamento medicamento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_id", nullable = false) // FK -> sucursal donde ocurrió el movimiento
    private Sucursal sucursal;

    @Column(name = "tipo_movimiento", nullable = false)
    private Short tipoMovimiento; // Ver constantes arriba (0-6)

    @Column(nullable = false)
    private Integer cantidad; // Entero positivo (RN-CU13-01)

    @Column(name = "stock_anterior", nullable = false)
    private Integer stockAnterior; // Stock justo antes del movimiento (para trazabilidad)

    @Column(name = "stock_nuevo", nullable = false)
    private Integer stockNuevo; // Stock resultante después del movimiento

    @Column(name = "costo_unitario", precision = 10, scale = 2)
    private BigDecimal costoUnitario; // Obligatorio solo para tipo Compra (RN-CU13-01)

    @Column(name = "numero_referencia", length = 100)
    private String numeroReferencia; // Etiqueta dinámica según tipo (Factura/Devolución/Venta/Reclamo)

    @Column(columnDefinition = "TEXT")
    private String motivo; // Obligatorio para Ajuste+/Ajuste-/Devolución/Reclamo (RN-CU13-01)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false) // FK -> usuario que registró el movimiento
    private Usuario usuario;

    @Column(nullable = false)
    private Boolean activo = true; // Permite desactivar/activar el registro desde el listado (CU-13)

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();
}
