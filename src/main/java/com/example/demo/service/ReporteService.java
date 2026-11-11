package com.example.demo.service;

import com.example.demo.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

// Motor de reportes: cada reporte es una definición (Def) con su SQL base.
// Todos devuelven columnas: label, total y (opcional) monto.
@Service
@RequiredArgsConstructor
public class ReporteService {

    private final JdbcTemplate jdbc;

    // ------------------------------------------------------------------
    // Definición de un reporte
    // ------------------------------------------------------------------
    private static final class Def {
        final String clave, categoria, titulo, ejeX, ejeY;
        String select, fromSql, fechaCol, sedeCol, whereSql, tailSql, extraLbl;
        boolean tiempo, comparar, general;

        Def(String clave, String categoria, String titulo, String ejeX, String ejeY) {
            this.clave = clave; this.categoria = categoria; this.titulo = titulo;
            this.ejeX = ejeX; this.ejeY = ejeY;
        }
        Def sel(String s) { this.select = s; return this; }
        Def from(String s) { this.fromSql = s; return this; }
        Def fecha(String s) { this.fechaCol = s; return this; }
        Def sede(String s) { this.sedeCol = s; return this; }
        Def where(String s) { this.whereSql = s; return this; }
        Def tail(String s) { this.tailSql = s; return this; }
        Def extra(String s) { this.extraLbl = s; return this; }
        Def tiempo() { this.tiempo = true; return this; }
        Def comparar() { this.comparar = true; return this; }
        Def general() { this.general = true; return this; }
    }

    private static Def d(String clave, String cat, String titulo, String x, String y) {
        return new Def(clave, cat, titulo, x, y);
    }

    // ------------------------------------------------------------------
    // Fragmentos comunes
    // ------------------------------------------------------------------
    private static final String CNT = "COUNT(*) AS total";
    private static final String CITA = "cita c JOIN estado_cita e ON e.id = c.estado_cita_id";
    private static final String NO_CANC = "e.nombre <> 'Cancelada'";
    private static final String PERDIDAS = "e.nombre IN ('Cancelada','No Asistió')";
    private static final String SERIE = "GROUP BY {B} ORDER BY {B}";

    private static String rank(String groupExpr) {
        return "GROUP BY " + groupExpr + " ORDER BY total DESC, 1 LIMIT 15";
    }

    private static final String DOW =
            "(ARRAY['Lunes','Martes','Miércoles','Jueves','Viernes','Sábado','Domingo'])"
                    + "[EXTRACT(ISODOW FROM c.fecha_hora)::int]";

    private static final String DESPACHO_DET =
            "detalle_despacho_medicamento dd JOIN despacho_medicamento dm ON dm.id = dd.despacho_id"
                    + " JOIN medicamento m ON m.id = COALESCE(dd.medicamento_sustituto_id, dd.medicamento_id)";

    private static final String LAB_DET =
            "detalle_orden_laboratorio dl JOIN orden_laboratorio o ON o.id = dl.orden_id"
                    + " JOIN examen_laboratorio x ON x.id = dl.examen_id JOIN usuario med ON med.id = o.medico_id";

    private static final String LAB_ORD =
            "orden_laboratorio o JOIN usuario med ON med.id = o.medico_id";

    // Pago con su sede resuelta (cita, orden de laboratorio o despacho de farmacia)
    private static final String PAGO =
            "(SELECT p.*, COALESCE(c.sucursal_id,"
                    + " (SELECT MIN(med.sucursal_id) FROM orden_laboratorio o JOIN usuario med ON med.id = o.medico_id WHERE o.pago_id = p.id),"
                    + " (SELECT MIN(dm.sucursal_id) FROM despacho_medicamento dm WHERE dm.pago_id = p.id)) AS sede"
                    + " FROM pago p LEFT JOIN cita c ON c.id = p.cita_id) p";

    // ------------------------------------------------------------------
    // CATÁLOGO DE REPORTES
    // ------------------------------------------------------------------
    private static final List<Def> DEFS = List.of(

            // ===================== CITAS =====================
            d("citas_tiempo", "Citas", "Citas en el tiempo", "Periodo", "Citas")
                    .sel("to_char({B}, '{F}') AS label, " + CNT).from(CITA).fecha("c.fecha_hora")
                    .sede("c.sucursal_id").where(NO_CANC).tail(SERIE).tiempo(),
            d("citas_estado", "Citas", "Citas por estado", "Estado", "Citas")
                    .sel("e.nombre AS label, " + CNT).from(CITA).fecha("c.fecha_hora")
                    .sede("c.sucursal_id").tail(rank("e.nombre")),
            d("citas_top_dias", "Citas", "Días de mayor demanda", "Fecha", "Citas")
                    .sel("to_char(c.fecha_hora,'DD/MM/YYYY') AS label, " + CNT).from(CITA).fecha("c.fecha_hora")
                    .sede("c.sucursal_id").where(NO_CANC).tail(rank("1")),
            d("citas_dia_semana", "Citas", "Demanda por día de la semana", "Día", "Citas")
                    .sel(DOW + " AS label, " + CNT).from(CITA).fecha("c.fecha_hora")
                    .sede("c.sucursal_id").where(NO_CANC)
                    .tail("GROUP BY EXTRACT(ISODOW FROM c.fecha_hora)::int ORDER BY EXTRACT(ISODOW FROM c.fecha_hora)::int"),
            d("citas_hora", "Citas", "Demanda por hora del día", "Hora", "Citas")
                    .sel("to_char(c.fecha_hora,'HH24') || ':00' AS label, " + CNT).from(CITA).fecha("c.fecha_hora")
                    .sede("c.sucursal_id").where(NO_CANC).tail("GROUP BY 1 ORDER BY 1"),
            d("citas_sede", "Citas", "Citas por sede", "Sede", "Citas")
                    .sel("s.nombre AS label, " + CNT).from(CITA + " JOIN sucursal s ON s.id = c.sucursal_id")
                    .fecha("c.fecha_hora").sede("c.sucursal_id").where(NO_CANC).tail(rank("s.nombre")),
            d("citas_origen", "Citas", "Citas presenciales (walk-in) vs agendadas", "Origen", "Citas")
                    .sel("CASE WHEN c.es_walk_in THEN 'Presencial (walk-in)' ELSE 'Agendada en portal' END AS label, " + CNT)
                    .from(CITA).fecha("c.fecha_hora").sede("c.sucursal_id").where(NO_CANC).tail(rank("1")),
            d("citas_emergencia", "Citas", "Citas de emergencia vs normales", "Prioridad", "Citas")
                    .sel("CASE WHEN c.es_emergencia THEN 'Emergencia' ELSE 'Normal' END AS label, " + CNT)
                    .from(CITA).fecha("c.fecha_hora").sede("c.sucursal_id").where(NO_CANC).tail(rank("1")),
            d("citas_perdidas_esp", "Citas", "Canceladas / No asistió por especialidad", "Especialidad", "Citas")
                    .sel("s.nombre AS label, " + CNT)
                    .from(CITA + " JOIN especialidad s ON s.id = c.especialidad_id").fecha("c.fecha_hora")
                    .sede("c.sucursal_id").where(PERDIDAS).tail(rank("s.nombre")),
            d("citas_reasignaciones", "Citas", "Reasignaciones de médico (por médico saliente)", "Médico", "Reasignaciones")
                    .sel("m.nombre_completo AS label, " + CNT)
                    .from("reasignacion_medico r JOIN cita c ON c.id = r.cita_id JOIN usuario m ON m.id = r.medico_anterior_id")
                    .fecha("r.creado_en").sede("c.sucursal_id").tail(rank("m.nombre_completo")),
            d("citas_seguimiento", "Citas", "Citas de seguimiento por tipo", "Tipo", "Seguimientos")
                    .sel("CASE cs.tipo_seguimiento WHEN 'MONITOREO_TRATAMIENTO' THEN 'Monitoreo de tratamiento' ELSE 'Revisión de resultados' END AS label, " + CNT)
                    .from("cita_seguimiento cs JOIN cita c ON c.id = cs.cita_nueva_id").fecha("cs.creado_en")
                    .sede("c.sucursal_id").tail(rank("cs.tipo_seguimiento")),

            // ============ MÉDICOS Y ESPECIALIDADES ============
            d("med_citas", "Médicos y especialidades", "Médicos con más citas", "Médico", "Citas")
                    .sel("m.nombre_completo AS label, " + CNT).from(CITA + " JOIN usuario m ON m.id = c.medico_id")
                    .fecha("c.fecha_hora").sede("c.sucursal_id").where(NO_CANC).tail(rank("m.nombre_completo")),
            d("med_consultas", "Médicos y especialidades", "Médicos con más consultas finalizadas", "Médico", "Consultas")
                    .sel("m.nombre_completo AS label, " + CNT)
                    .from("consulta_medica cm JOIN cita c ON c.id = cm.cita_id JOIN usuario m ON m.id = cm.medico_id")
                    .fecha("cm.creado_en").sede("c.sucursal_id").where("cm.finalizada = TRUE").tail(rank("m.nombre_completo")),
            d("med_perdidas", "Médicos y especialidades", "Médicos con más citas canceladas / no asistió", "Médico", "Citas")
                    .sel("m.nombre_completo AS label, " + CNT).from(CITA + " JOIN usuario m ON m.id = c.medico_id")
                    .fecha("c.fecha_hora").sede("c.sucursal_id").where(PERDIDAS).tail(rank("m.nombre_completo")),
            d("med_emergencias", "Médicos y especialidades", "Médicos con más emergencias atendidas", "Médico", "Emergencias")
                    .sel("m.nombre_completo AS label, " + CNT).from(CITA + " JOIN usuario m ON m.id = c.medico_id")
                    .fecha("c.fecha_hora").sede("c.sucursal_id").where(NO_CANC + " AND c.es_emergencia = TRUE")
                    .tail(rank("m.nombre_completo")),
            d("med_ingresos", "Médicos y especialidades", "Ingresos por médico (consultas pagadas)", "Médico", "Pagos")
                    .sel("m.nombre_completo AS label, COUNT(*) AS total, SUM(p.monto_total) AS monto")
                    .from("pago p JOIN cita c ON c.id = p.cita_id JOIN usuario m ON m.id = c.medico_id")
                    .fecha("p.creado_en").sede("c.sucursal_id").tail(rank("m.nombre_completo")).extra("Ingresos (Q)"),
            d("esp_citas", "Médicos y especialidades", "Especialidades más frecuentes", "Especialidad", "Citas")
                    .sel("s.nombre AS label, " + CNT).from(CITA + " JOIN especialidad s ON s.id = c.especialidad_id")
                    .fecha("c.fecha_hora").sede("c.sucursal_id").where(NO_CANC).tail(rank("s.nombre")),
            d("esp_ingresos", "Médicos y especialidades", "Ingresos por especialidad", "Especialidad", "Pagos")
                    .sel("s.nombre AS label, COUNT(*) AS total, SUM(p.monto_total) AS monto")
                    .from("pago p JOIN cita c ON c.id = p.cita_id JOIN especialidad s ON s.id = c.especialidad_id")
                    .fecha("p.creado_en").sede("c.sucursal_id").tail(rank("s.nombre")).extra("Ingresos (Q)"),
            d("esp_pacientes", "Médicos y especialidades", "Pacientes distintos por especialidad", "Especialidad", "Pacientes")
                    .sel("s.nombre AS label, COUNT(DISTINCT c.paciente_id) AS total")
                    .from(CITA + " JOIN especialidad s ON s.id = c.especialidad_id")
                    .fecha("c.fecha_hora").sede("c.sucursal_id").where(NO_CANC).tail(rank("s.nombre")),

            // ===================== CLÍNICA =====================
            d("cli_cie10", "Clínica", "Diagnósticos más frecuentes (CIE-10)", "Diagnóstico", "Consultas")
                    .sel("COALESCE(x.codigo || ' - ' || x.descripcion, 'Sin código CIE-10') AS label, " + CNT)
                    .from("consulta_medica cm JOIN cita c ON c.id = cm.cita_id LEFT JOIN cie10 x ON x.id = cm.cie10_id")
                    .fecha("cm.creado_en").sede("c.sucursal_id").tail(rank("x.codigo, x.descripcion")),
            d("cli_consulta_estado", "Clínica", "Consultas finalizadas vs en curso", "Estado", "Consultas")
                    .sel("CASE WHEN cm.finalizada THEN 'Finalizada' ELSE 'En curso' END AS label, " + CNT)
                    .from("consulta_medica cm JOIN cita c ON c.id = cm.cita_id").fecha("cm.creado_en")
                    .sede("c.sucursal_id").tail(rank("1")),
            d("cli_signos_emergencia", "Clínica", "Signos vitales: emergencia vs normal", "Tipo", "Registros")
                    .sel("CASE WHEN sv.es_emergencia THEN 'Emergencia' ELSE 'Normal' END AS label, " + CNT)
                    .from("signos_vitales sv JOIN cita c ON c.id = sv.cita_id").fecha("sv.creado_en")
                    .sede("c.sucursal_id").tail(rank("1")),
            d("cli_alertas", "Clínica", "Signos vitales fuera de rango clínico (por indicador)", "Indicador", "Alertas")
                    .sel("a.ind AS label, " + CNT)
                    .from("(SELECT c.sucursal_id, sv.creado_en, v.ind FROM signos_vitales sv JOIN cita c ON c.id = sv.cita_id"
                            + " CROSS JOIN LATERAL (VALUES"
                            + " ('Presión arterial', (sv.presion_sistolica NOT BETWEEN 90 AND 140 OR sv.presion_diastolica NOT BETWEEN 60 AND 90)),"
                            + " ('Temperatura', (sv.temperatura NOT BETWEEN 36.0 AND 37.5)),"
                            + " ('Frecuencia cardíaca', (sv.frecuencia_cardiaca NOT BETWEEN 60 AND 100))"
                            + ") AS v(ind, fuera) WHERE v.fuera) a")
                    .fecha("a.creado_en").sede("a.sucursal_id").tail(rank("a.ind")),
            d("cli_enfermeros", "Clínica", "Enfermeros con más signos vitales registrados", "Enfermero", "Registros")
                    .sel("u.nombre_completo AS label, " + CNT)
                    .from("signos_vitales sv JOIN cita c ON c.id = sv.cita_id JOIN usuario u ON u.id = sv.enfermero_id")
                    .fecha("sv.creado_en").sede("c.sucursal_id").tail(rank("u.nombre_completo")),

            // ==================== LABORATORIO ====================
            d("lab_examenes", "Laboratorio", "Exámenes más solicitados", "Examen", "Solicitudes")
                    .sel("x.nombre AS label, COUNT(*) AS total, SUM(dl.precio_unitario) AS monto")
                    .from(LAB_DET).fecha("o.creado_en").sede("med.sucursal_id").where("o.estado <> 3")
                    .tail(rank("x.nombre")).extra("Monto (Q)"),
            d("lab_fuera_rango", "Laboratorio", "Exámenes con más resultados fuera de rango", "Examen", "Resultados fuera de rango")
                    .sel("x.nombre AS label, " + CNT).from(LAB_DET).fecha("o.creado_en").sede("med.sucursal_id")
                    .where("o.estado <> 3 AND dl.fuera_de_rango = TRUE").tail(rank("x.nombre")),
            d("lab_estado", "Laboratorio", "Órdenes de laboratorio por estado", "Estado", "Órdenes")
                    .sel("CASE o.estado WHEN 0 THEN 'Pendiente' WHEN 1 THEN 'En proceso' WHEN 2 THEN 'Completada' ELSE 'Cancelada' END AS label, " + CNT)
                    .from(LAB_ORD).fecha("o.creado_en").sede("med.sucursal_id")
                    .tail("GROUP BY o.estado ORDER BY o.estado"),
            d("lab_externa", "Laboratorio", "Órdenes internas vs externas", "Tipo", "Órdenes")
                    .sel("CASE WHEN o.es_externa THEN 'Externa' ELSE 'Interna' END AS label, " + CNT)
                    .from(LAB_ORD).fecha("o.creado_en").sede("med.sucursal_id").where("o.estado <> 3").tail(rank("1")),
            d("lab_medicos", "Laboratorio", "Médicos que más solicitan laboratorio", "Médico", "Órdenes")
                    .sel("med.nombre_completo AS label, COUNT(*) AS total, SUM(o.monto_total) AS monto")
                    .from(LAB_ORD).fecha("o.creado_en").sede("med.sucursal_id").where("o.estado <> 3")
                    .tail(rank("med.nombre_completo")).extra("Monto (Q)"),
            d("lab_publicacion", "Laboratorio", "Resultados publicados vs pendientes de publicar", "Estado", "Exámenes")
                    .sel("CASE WHEN dl.publicado THEN 'Publicado' ELSE 'Pendiente de publicar' END AS label, " + CNT)
                    .from(LAB_DET).fecha("o.creado_en").sede("med.sucursal_id").where("o.estado IN (1,2)").tail(rank("1")),

            // ==================== FARMACIA ====================
            d("far_despachados", "Farmacia", "Medicamentos más despachados", "Medicamento", "Unidades")
                    .sel("m.nombre AS label, SUM(dd.cantidad) AS total, SUM(dd.cantidad * dd.precio_unitario) AS monto")
                    .from(DESPACHO_DET).fecha("dm.creado_en").sede("dm.sucursal_id")
                    .tail(rank("m.nombre")).extra("Ventas (Q)"),
            d("far_controlados", "Farmacia", "Medicamentos controlados despachados", "Medicamento", "Unidades")
                    .sel("m.nombre AS label, SUM(dd.cantidad) AS total")
                    .from(DESPACHO_DET).fecha("dm.creado_en").sede("dm.sucursal_id")
                    .where("m.es_controlado = TRUE").tail(rank("m.nombre")),
            d("far_sustituidos", "Farmacia", "Medicamentos más sustituidos", "Medicamento", "Sustituciones")
                    .sel("m.nombre AS label, " + CNT)
                    .from("detalle_despacho_medicamento dd JOIN despacho_medicamento dm ON dm.id = dd.despacho_id JOIN medicamento m ON m.id = dd.medicamento_id")
                    .fecha("dm.creado_en").sede("dm.sucursal_id").where("dd.sustituido = TRUE").tail(rank("m.nombre")),
            d("far_recetados", "Farmacia", "Medicamentos más recetados", "Medicamento", "Veces recetado")
                    .sel("m.nombre AS label, " + CNT)
                    .from("detalle_receta_medica dr JOIN receta_medica r ON r.id = dr.receta_id"
                            + " JOIN consulta_medica cm ON cm.id = r.consulta_id JOIN cita c ON c.id = cm.cita_id"
                            + " JOIN medicamento m ON m.id = dr.medicamento_id")
                    .fecha("r.creado_en").sede("c.sucursal_id").tail(rank("m.nombre")),
            d("far_ventas_tiempo", "Farmacia", "Despachos y ventas en el tiempo", "Periodo", "Despachos")
                    .sel("to_char({B}, '{F}') AS label, COUNT(*) AS total, SUM(dm.monto_total) AS monto")
                    .from("despacho_medicamento dm").fecha("dm.creado_en").sede("dm.sucursal_id")
                    .tail(SERIE).tiempo().extra("Ventas (Q)"),
            d("far_tipo", "Farmacia", "Despachos con receta vs venta libre", "Tipo", "Despachos")
                    .sel("CASE WHEN dm.es_venta_libre THEN 'Venta libre' ELSE 'Con receta' END AS label, COUNT(*) AS total, SUM(dm.monto_total) AS monto")
                    .from("despacho_medicamento dm").fecha("dm.creado_en").sede("dm.sucursal_id")
                    .tail(rank("1")).extra("Ventas (Q)"),
            d("far_farmaceutico", "Farmacia", "Despachos por farmacéutico", "Farmacéutico", "Despachos")
                    .sel("u.nombre_completo AS label, COUNT(*) AS total, SUM(dm.monto_total) AS monto")
                    .from("despacho_medicamento dm JOIN usuario u ON u.id = dm.farmaceutico_id")
                    .fecha("dm.creado_en").sede("dm.sucursal_id").tail(rank("u.nombre_completo")).extra("Ventas (Q)"),
            d("far_stock_bajo", "Farmacia", "Inventario: medicamentos con menor stock vs mínimo", "Medicamento", "Stock actual")
                    .sel("m.nombre AS label, SUM(i.stock_actual) AS total, COALESCE(m.stock_minimo,0) AS monto")
                    .from("inventario_medicamento i JOIN medicamento m ON m.id = i.medicamento_id")
                    .sede("i.sucursal_id").where("m.estado = 1")
                    .tail("GROUP BY m.nombre, m.stock_minimo ORDER BY total ASC, 1 LIMIT 15")
                    .extra("Stock mínimo").comparar(),
            d("far_valor_inventario", "Farmacia", "Valor del inventario por medicamento", "Medicamento", "Unidades en stock")
                    .sel("m.nombre AS label, SUM(i.stock_actual) AS total, SUM(i.stock_actual * m.precio) AS monto")
                    .from("inventario_medicamento i JOIN medicamento m ON m.id = i.medicamento_id")
                    .sede("i.sucursal_id").where("m.estado = 1")
                    .tail("GROUP BY m.nombre ORDER BY monto DESC, 1 LIMIT 15").extra("Valor (Q)"),
            d("far_movimientos", "Farmacia", "Movimientos de inventario por tipo", "Tipo de movimiento", "Movimientos")
                    .sel("CASE mi.tipo_movimiento WHEN 0 THEN 'Compra' WHEN 1 THEN 'Devolución' WHEN 2 THEN 'Venta' WHEN 3 THEN 'Reclamo' WHEN 4 THEN 'Ajuste +' WHEN 5 THEN 'Ajuste -' ELSE 'Despacho' END AS label, COUNT(*) AS total, SUM(mi.cantidad) AS monto")
                    .from("movimiento_inventario mi").fecha("mi.creado_en").sede("mi.sucursal_id")
                    .where("mi.activo = TRUE").tail("GROUP BY mi.tipo_movimiento ORDER BY mi.tipo_movimiento").extra("Unidades"),

            // ==================== FINANZAS ====================
            d("fin_metodo", "Finanzas", "Métodos de pago", "Método", "Pagos")
                    .sel("CASE p.metodo_pago WHEN 'EFECTIVO' THEN 'Efectivo' WHEN 'VISA' THEN 'Visa' WHEN 'MASTERCARD' THEN 'Mastercard'"
                            + " WHEN 'DEBITO' THEN 'Débito' WHEN 'TARJETA' THEN 'Tarjeta en línea' WHEN 'PASARELA_LINEA' THEN 'Pasarela en línea'"
                            + " ELSE p.metodo_pago END AS label, COUNT(*) AS total, SUM(p.monto_total) AS monto")
                    .from(PAGO).fecha("p.creado_en").sede("p.sede").tail(rank("p.metodo_pago")).extra("Monto (Q)"),
            d("fin_tiempo", "Finanzas", "Ingresos en el tiempo", "Periodo", "Pagos")
                    .sel("to_char({B}, '{F}') AS label, COUNT(*) AS total, SUM(p.monto_total) AS monto")
                    .from(PAGO).fecha("p.creado_en").sede("p.sede").tail(SERIE).tiempo().extra("Ingresos (Q)"),
            d("fin_origen", "Finanzas", "Ingresos por origen (consulta / laboratorio / farmacia)", "Origen", "Pagos")
                    .sel("CASE WHEN p.cita_id IS NOT NULL THEN 'Consultas'"
                            + " WHEN EXISTS (SELECT 1 FROM orden_laboratorio ol WHERE ol.pago_id = p.id) THEN 'Laboratorio'"
                            + " WHEN EXISTS (SELECT 1 FROM despacho_medicamento dx WHERE dx.pago_id = p.id) THEN 'Farmacia'"
                            + " ELSE 'Otros' END AS label, COUNT(*) AS total, SUM(p.monto_total) AS monto")
                    .from(PAGO).fecha("p.creado_en").sede("p.sede").tail(rank("1")).extra("Ingresos (Q)"),
            d("fin_cajero", "Finanzas", "Cobros por cajero", "Cajero", "Cobros")
                    .sel("COALESCE(u.nombre_completo, 'Pago en línea') AS label, COUNT(*) AS total, SUM(p.monto_total) AS monto")
                    .from(PAGO + " LEFT JOIN usuario u ON u.id = p.cajero_id").fecha("p.creado_en").sede("p.sede")
                    .tail(rank("u.nombre_completo")).extra("Monto (Q)"),
            d("fin_sede", "Finanzas", "Ingresos por sede", "Sede", "Pagos")
                    .sel("COALESCE(s.nombre, 'Sin sede') AS label, COUNT(*) AS total, SUM(p.monto_total) AS monto")
                    .from(PAGO + " LEFT JOIN sucursal s ON s.id = p.sede").fecha("p.creado_en").sede("p.sede")
                    .tail(rank("s.nombre")).extra("Ingresos (Q)"),

            // ================== ADMINISTRACIÓN ==================
            d("adm_usuarios_rol", "Administración", "Usuarios activos por rol (estado actual)", "Rol", "Usuarios")
                    .sel("r.nombre AS label, " + CNT).from("usuario u JOIN rol r ON r.id = u.rol_id")
                    .sede("u.sucursal_id").where("u.estado = 1").tail(rank("r.nombre")),
            d("adm_agenda", "Administración", "Eventos de agenda médica por tipo", "Tipo de evento", "Eventos")
                    .sel("CASE ev.tipo_evento WHEN 0 THEN 'Reunión' WHEN 1 THEN 'Descanso' WHEN 2 THEN 'Capacitación' WHEN 3 THEN 'Personal' ELSE 'Otro' END AS label, " + CNT)
                    .from("evento_agenda ev JOIN usuario m ON m.id = ev.medico_id").fecha("ev.fecha_inicio")
                    .sede("m.sucursal_id").tail("GROUP BY ev.tipo_evento ORDER BY ev.tipo_evento"),
            d("adm_auditoria", "Administración", "Actividad de auditoría por tabla y acción", "Tabla / Acción", "Operaciones")
                    .sel("COALESCE(a.tabla_afectada,'N/D') || ' / ' || a.accion AS label, " + CNT)
                    .from("log_auditoria a").fecha("a.creado_en")
                    .tail(rank("a.tabla_afectada, a.accion")).general()
    );

    // ------------------------------------------------------------------
    // API
    // ------------------------------------------------------------------
    public List<Map<String, Object>> catalogo(boolean general) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Def df : DEFS) {
            if (df.general && !general) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("clave", df.clave);
            m.put("categoria", df.categoria);
            m.put("titulo", df.titulo);
            m.put("usaFecha", df.fechaCol != null);
            out.add(m);
        }
        return out;
    }

    public Map<String, Object> generar(String clave, LocalDateTime desde, LocalDateTime hasta,
                                       Integer sede, boolean general) {
        Def df = DEFS.stream().filter(x -> x.clave.equals(clave)).findFirst()
                .orElseThrow(() -> new ValidationException("El reporte solicitado no existe."));
        if (df.general && !general) {
            throw new ValidationException("Este reporte no está disponible para su rol.");
        }
        return ejecutar(df, desde, hasta, sede);
    }

    public List<Map<String, Object>> todos(LocalDateTime desde, LocalDateTime hasta, Integer sede, boolean general) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Def df : DEFS) {
            if (df.general && !general) continue;
            out.add(ejecutar(df, desde, hasta, sede));
        }
        return out;
    }

    // ------------------------------------------------------------------
    private Map<String, Object> ejecutar(Def df, LocalDateTime desde, LocalDateTime hasta, Integer sede) {
        boolean usaFecha = df.fechaCol != null;
        if (usaFecha && (desde == null || hasta == null || hasta.isBefore(desde))) {
            throw new ValidationException("El rango de fechas no es válido.");
        }

        StringBuilder sql = new StringBuilder("SELECT ").append(df.select)
                .append(" FROM ").append(df.fromSql).append(" WHERE ");
        List<Object> args = new ArrayList<>();

        if (usaFecha) {
            sql.append(df.fechaCol).append(" BETWEEN ? AND ?");
            args.add(Timestamp.valueOf(desde));
            args.add(Timestamp.valueOf(hasta));
        } else {
            sql.append("1=1");
        }
        if (df.whereSql != null) sql.append(" AND ").append(df.whereSql);
        if (df.sedeCol != null && sede != null) {
            sql.append(" AND ").append(df.sedeCol).append(" = ?");
            args.add(sede);
        }
        sql.append(" ").append(df.tailSql);

        String unidad = null;
        String q = sql.toString();
        if (df.tiempo) {
            long horas = Duration.between(desde, hasta).toHours();
            unidad = horas <= 48 ? "hour" : (horas <= 92 * 24 ? "day" : "month");
            String formato = switch (unidad) {
                case "hour" -> "DD/MM HH24\"h\"";
                case "day" -> "DD/MM/YYYY";
                default -> "MM/YYYY";
            };
            q = q.replace("{B}", "date_trunc('" + unidad + "', " + df.fechaCol + ")").replace("{F}", formato);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("clave", df.clave);
        out.put("categoria", df.categoria);
        out.put("titulo", df.titulo);
        out.put("ejeX", df.ejeX);
        out.put("ejeY", df.ejeY);
        out.put("ejeExtra", df.extraLbl);
        out.put("comparar", df.comparar);
        out.put("usaFecha", usaFecha);
        out.put("unidad", unidad);
        out.put("datos", jdbc.queryForList(q, args.toArray()));
        return out;
    }
}