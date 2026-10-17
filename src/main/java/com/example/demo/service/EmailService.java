package com.example.demo.service;

import com.example.demo.modelo.Cita;
import com.example.demo.modelo.CitaSeguimiento;
import com.example.demo.modelo.ConsultaMedica;
import com.example.demo.modelo.DetalleOrdenLaboratorio;
import com.example.demo.modelo.DetalleRecetaMedica;
import com.example.demo.modelo.OrdenLaboratorio;
import com.example.demo.modelo.Pago;
import com.example.demo.modelo.RecetaMedica;
import com.example.demo.modelo.SignosVitales;
import com.example.demo.modelo.Usuario;
import jakarta.annotation.PreDestroy;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final JavaMailSender mailSender;

    // Envío en segundo plano: un SMTP lento no bloquea la respuesta HTTP.
    private final ExecutorService ejecutor = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "email-sender");
        t.setDaemon(true);
        return t;
    });

    @Value("${spring.mail.username}")
    private String remitente;

    @Value("${app.hospital.nombre}")
    private String nombreHospital;

    @Value("${app.hospital.telefono}")
    private String telefonoHospital;

    @PreDestroy
    void cerrar() {
        ejecutor.shutdown();
    }

    // =====================================================================
    // CUENTAS
    // =====================================================================

    // Bienvenida al registrarse en el portal (CU-02, RN-GLOBAL-006)
    public void enviarBienvenida(Usuario paciente) {
        enviarPlantilla(paciente.getCorreoElectronico(),
                "Bienvenido al Sistema de Citas - Hospital " + nombreHospital,
                "Bienvenido(a)",
                "Estimado(a) <b>" + esc(paciente.getNombreCompleto()) + "</b>, su registro ha sido completado exitosamente. "
                        + "Ya puede agendar sus citas médicas a través de nuestro portal.",
                java.util.Collections.singletonList(fila("Usuario", paciente.getNombreUsuario(), true)),
                null);
    }

    // Cuenta creada por un administrador (CU-01). No incluye la contraseña por seguridad.
    public void enviarCuentaCreadaPorAdmin(Usuario u) {
        List<String[]> filas = new ArrayList<>();
        filas.add(fila("Usuario", u.getNombreUsuario(), true));
        filas.add(fila("Rol", u.getRol().getNombre(), false));
        if (u.getSucursal() != null) filas.add(fila("Sucursal", u.getSucursal().getNombre(), false));
        if (u.getEspecialidad() != null) filas.add(fila("Especialidad", u.getEspecialidad().getNombre(), false));

        enviarPlantilla(u.getCorreoElectronico(),
                "Cuenta Creada - Hospital " + nombreHospital,
                "Su cuenta ha sido creada",
                "Estimado(a) <b>" + esc(u.getNombreCompleto()) + "</b>, un administrador ha creado una cuenta para usted "
                        + "en el Sistema Informático Hospitalario.",
                filas,
                "Su contraseña le fue proporcionada por el administrador. Le recomendamos mantenerla en confidencialidad.");
    }

    // =====================================================================
    // CITAS
    // =====================================================================

    // Cita agendada, pendiente de pago (CU-03)
    public void enviarCitaAgendada(Cita cita) {
        List<String[]> filas = filasCita(cita);
        filas.add(fila("Monto a pagar", "Q" + cita.getMonto(), true));
        enviarPlantilla(cita.getPaciente().getCorreoElectronico(),
                "Cita Agendada - Pendiente de Pago - Hospital " + nombreHospital,
                "Cita agendada",
                "Estimado(a) <b>" + esc(cita.getPaciente().getNombreCompleto()) + "</b>, su cita fue registrada y está "
                        + "<b>pendiente de pago</b>. Se confirmará una vez realizado el pago.",
                filas, null);
    }

    // Comprobante de pago de la cita (en línea CU-04 o en caja CU-06)
    public void enviarComprobantePago(Cita cita, Pago pago) {
        List<String[]> filas = filasPago(pago);
        filas.addAll(filasCita(cita));
        enviarPlantilla(cita.getPaciente().getCorreoElectronico(),
                "Comprobante de Pago - Cita Médica - Hospital " + nombreHospital,
                "Cita confirmada",
                "Estimado(a) <b>" + esc(cita.getPaciente().getNombreCompleto()) + "</b>, hemos recibido su pago y su cita "
                        + "quedó confirmada.",
                filas, "Por favor preséntese 15 minutos antes de su cita con su DPI.");
    }

    // Llegada registrada en recepción (CU-05)
    public void enviarLlegadaRegistrada(Cita cita) {
        enviarPlantilla(cita.getPaciente().getCorreoElectronico(),
                "Llegada Registrada - Hospital " + nombreHospital,
                "Llegada registrada",
                "Estimado(a) <b>" + esc(cita.getPaciente().getNombreCompleto()) + "</b>, hemos registrado su llegada"
                        + (cita.getHoraLlegada() != null ? " a las " + cita.getHoraLlegada().format(FORMATO_FECHA) : "") + ".",
                filasCita(cita),
                cita.isEsEmergencia()
                        ? "Su caso fue marcado como emergencia y pasará directamente a toma de signos vitales."
                        : "Por favor espere en la sala de espera, será llamado para la toma de signos vitales.");
    }

    // Cita cancelada (recepción, inasistencia, expiración de pago)
    public void enviarCitaCancelada(Cita cita, String motivo) {
        List<String[]> filas = new ArrayList<>();
        if (motivo != null && !motivo.isBlank()) filas.add(fila("Motivo", motivo, true));
        filas.addAll(filasCita(cita));
        enviarPlantilla(cita.getPaciente().getCorreoElectronico(),
                "Cita Cancelada - Hospital " + nombreHospital,
                "Cita cancelada",
                "Estimado(a) <b>" + esc(cita.getPaciente().getNombreCompleto()) + "</b>, su cita #" + cita.getId()
                        + " ha sido cancelada.",
                filas, "Si desea atenderse, puede agendar una nueva cita a través del portal.");
    }

    // Cita reagendada por el paciente
    public void enviarCitaReagendada(Cita cita, LocalDateTime fechaAnterior) {
        List<String[]> filas = new ArrayList<>();
        filas.add(fila("Fecha anterior", fechaAnterior.format(FORMATO_FECHA), false));
        filas.add(fila("Nueva fecha", cita.getFechaHora().format(FORMATO_FECHA), true));
        filas.addAll(filasCita(cita));
        enviarPlantilla(cita.getPaciente().getCorreoElectronico(),
                "Cita Reagendada - Hospital " + nombreHospital,
                "Cita reagendada",
                "Estimado(a) <b>" + esc(cita.getPaciente().getNombreCompleto()) + "</b>, su cita #" + cita.getId()
                        + " fue reagendada.",
                filas, null);
    }

    // Reasignación de médico en recepción (FA07 CU-05)
    public void enviarMedicoReasignado(Cita cita, Usuario medicoAnterior) {
        List<String[]> filas = new ArrayList<>();
        filas.add(fila("Médico anterior", medicoAnterior.getNombreCompleto(), false));
        filas.add(fila("Médico actual", cita.getMedico().getNombreCompleto(), true));
        filas.add(fila("Especialidad", cita.getEspecialidad().getNombre(), false));
        filas.add(fila("Sucursal", cita.getSucursal().getNombre(), false));
        filas.add(fila("Fecha y hora", cita.getFechaHora().format(FORMATO_FECHA), false));
        enviarPlantilla(cita.getPaciente().getCorreoElectronico(),
                "Cambio de Médico en su Cita - Hospital " + nombreHospital,
                "Cambio de médico",
                "Estimado(a) <b>" + esc(cita.getPaciente().getNombreCompleto()) + "</b>, el médico asignado a su cita #"
                        + cita.getId() + " ha cambiado.",
                filas, null);
    }

    // =====================================================================
    // ATENCIÓN CLÍNICA
    // =====================================================================

    // Signos vitales registrados (CU-07)
    public void enviarSignosVitales(Cita cita, SignosVitales sv) {
        List<String[]> filas = new ArrayList<>();
        filas.add(fila("Presión arterial", sv.getPresionSistolica() + "/" + sv.getPresionDiastolica() + " mmHg", false));
        filas.add(fila("Temperatura", sv.getTemperatura() + " °C", false));
        filas.add(fila("Frecuencia cardíaca", sv.getFrecuenciaCardiaca() + " lpm", false));
        filas.add(fila("Peso", sv.getPeso() + " kg", false));
        filas.add(fila("Talla", sv.getTalla() + " cm", false));
        enviarPlantilla(cita.getPaciente().getCorreoElectronico(),
                "Signos Vitales Registrados - Hospital " + nombreHospital,
                "Signos vitales registrados",
                "Estimado(a) <b>" + esc(cita.getPaciente().getNombreCompleto()) + "</b>, se han registrado sus signos vitales.",
                filas,
                sv.isEsEmergencia()
                        ? "Su caso fue marcado como emergencia: pasará directamente a consulta médica."
                        : "Por favor regrese a la sala de espera, será llamado para su consulta médica.");
    }

    // Consulta médica finalizada (CU-08)
    public void enviarConsultaFinalizada(Cita cita, ConsultaMedica consulta) {
        List<String[]> filas = new ArrayList<>();
        filas.add(fila("Médico", cita.getMedico().getNombreCompleto(), false));
        filas.add(fila("Especialidad", cita.getEspecialidad().getNombre(), false));
        filas.add(fila("Diagnóstico", consulta.getDiagnostico(), true));
        filas.add(fila("Plan de tratamiento", consulta.getPlanTratamiento(), false));
        enviarPlantilla(cita.getPaciente().getCorreoElectronico(),
                "Consulta Médica Finalizada - Hospital " + nombreHospital,
                "Consulta finalizada",
                "Estimado(a) <b>" + esc(cita.getPaciente().getNombreCompleto()) + "</b>, su consulta médica ha finalizado.",
                filas,
                "Siga las indicaciones de su médico. Si se le generaron recetas u órdenes de laboratorio, las recibirá por correo.");
    }

    // Atención finalizada (cierre de la cita)
    public void enviarAtencionFinalizada(Cita cita) {
        enviarPlantilla(cita.getPaciente().getCorreoElectronico(),
                "Atención Finalizada - Hospital " + nombreHospital,
                "Atención finalizada",
                "Estimado(a) <b>" + esc(cita.getPaciente().getNombreCompleto()) + "</b>, su atención médica ha sido "
                        + "finalizada. Gracias por confiar en nosotros.",
                filasCita(cita), null);
    }

    // Cita de seguimiento agendada (CU-12): fecha, hora y razón
    public void enviarSeguimientoAgendado(Cita citaSeguimiento, CitaSeguimiento seguimiento) {
        String tipoLegible = CitaSeguimiento.MONITOREO_TRATAMIENTO.equals(seguimiento.getTipoSeguimiento())
                ? "Monitoreo de Tratamiento" : "Revisión de Resultados de Laboratorio";
        List<String[]> filas = new ArrayList<>();
        filas.add(fila("Tipo de seguimiento", tipoLegible, false));
        filas.add(fila("Médico", citaSeguimiento.getMedico().getNombreCompleto(), false));
        filas.add(fila("Sucursal", citaSeguimiento.getSucursal().getNombre(), false));
        filas.add(fila("Fecha", citaSeguimiento.getFechaHora().toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), true));
        filas.add(fila("Hora", citaSeguimiento.getFechaHora().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")), true));
        filas.add(fila("Razón", seguimiento.getMotivoSeguimiento(), false));
        filas.add(fila("Estado", "Pendiente de pago (Q" + citaSeguimiento.getMonto() + ")", true));

        enviarPlantilla(citaSeguimiento.getPaciente().getCorreoElectronico(),
                "Cita de Seguimiento Agendada - Hospital " + nombreHospital,
                "Cita de seguimiento agendada",
                "Estimado(a) <b>" + esc(citaSeguimiento.getPaciente().getNombreCompleto())
                        + "</b>, su médico agendó una cita de seguimiento. Para confirmarla debe realizar el pago en caja "
                        + "antes de finalizar el día de la cita; de lo contrario será cancelada.",
                filas, null);
    }

    // Recordatorio 1-2 días antes de la cita de seguimiento (CU-12, RN-CU11-05)
    public void enviarRecordatorioSeguimiento(Cita citaSeguimiento) {
        enviarPlantilla(citaSeguimiento.getPaciente().getCorreoElectronico(),
                "Recordatorio: Su Cita de Seguimiento Mañana",
                "Recordatorio de cita",
                "Estimado(a) <b>" + esc(citaSeguimiento.getPaciente().getNombreCompleto())
                        + "</b>, le recordamos su próxima cita de seguimiento.",
                filasCita(citaSeguimiento), null);
    }

    // =====================================================================
    // LABORATORIO
    // =====================================================================

    // Orden de laboratorio generada por el médico
    public void enviarOrdenLaboratorio(OrdenLaboratorio orden, List<String> examenes) {
        List<String[]> filas = new ArrayList<>();
        filas.add(fila("Número de orden", "#" + orden.getId(), true));
        filas.add(fila("Médico", orden.getMedico().getNombreCompleto(), false));
        filas.add(fila("Exámenes", String.join(", ", examenes), false));
        filas.add(fila("Monto total", "Q" + orden.getMontoTotal(), false));
        enviarPlantilla(orden.getPaciente().getCorreoElectronico(),
                "Orden de Laboratorio Generada - Hospital " + nombreHospital,
                "Orden de laboratorio",
                "Estimado(a) <b>" + esc(orden.getPaciente().getNombreCompleto())
                        + "</b>, su médico ha generado una orden de laboratorio.",
                filas,
                orden.isEsExterna()
                        ? "La orden es para realizarse en un laboratorio externo."
                        : "Debe pasar a caja a realizar el pago y luego al área de laboratorio.");
    }

    // Pago de laboratorio en caja (CU-10)
    public void enviarComprobantePagoLaboratorio(OrdenLaboratorio orden, Pago pago) {
        List<String[]> filas = filasPago(pago);
        filas.add(fila("Orden de laboratorio", "#" + orden.getId(), false));
        enviarPlantilla(orden.getPaciente().getCorreoElectronico(),
                "Comprobante de Pago - Laboratorio - Hospital " + nombreHospital,
                "Pago de laboratorio recibido",
                "Estimado(a) <b>" + esc(orden.getPaciente().getNombreCompleto())
                        + "</b>, hemos recibido el pago de su orden de laboratorio.",
                filas, "Ya puede pasar al área de laboratorio para la toma de muestras.");
    }

    // Resultado publicado (CU-09). No se envían los valores por correo, solo el aviso.
    public void enviarResultadoLaboratorioPublicado(OrdenLaboratorio orden, DetalleOrdenLaboratorio detalle,
                                                    boolean ordenCompletada) {
        List<String[]> filas = new ArrayList<>();
        filas.add(fila("Orden", "#" + orden.getId(), false));
        filas.add(fila("Examen", detalle.getExamen().getNombre(), true));
        enviarPlantilla(orden.getPaciente().getCorreoElectronico(),
                "Resultado de Laboratorio Disponible - Hospital " + nombreHospital,
                "Resultado disponible",
                "Estimado(a) <b>" + esc(orden.getPaciente().getNombreCompleto())
                        + "</b>, el resultado de su examen ya está disponible.",
                filas,
                "Puede consultarlo en el portal o con su médico."
                        + (ordenCompletada ? " Todos los exámenes de su orden han sido completados." : ""));
    }

    // =====================================================================
    // RECETA Y FARMACIA
    // =====================================================================

    // Receta médica enviada al guardarla (CU-08 FA04). Diseño formal con folio, Rx y firma.
    public void enviarReceta(RecetaMedica receta, List<DetalleRecetaMedica> detalles) {
        ConsultaMedica consulta = receta.getConsulta();
        Cita cita = consulta.getCita();

        StringBuilder filas = new StringBuilder();
        int i = 0;
        for (DetalleRecetaMedica d : detalles) {
            String fondo = (i++ % 2 == 0) ? "#ffffff" : BOX;
            filas.append("<tr style=\"background:").append(fondo).append("\">")
                    .append(tdReceta(d.getMedicamento().getNombre(), true))
                    .append(tdReceta(d.getDosis(), false))
                    .append(tdReceta(d.getFrecuencia(), false))
                    .append(tdReceta(d.getDuracion(), false))
                    .append(tdReceta(d.getIndicaciones() == null || d.getIndicaciones().isBlank() ? "-" : d.getIndicaciones(), false))
                    .append("</tr>");
        }

        String[] cabeceras = {"Medicamento", "Dosis", "Frecuencia", "Duración", "Indicaciones"};
        StringBuilder th = new StringBuilder();
        for (String c : cabeceras) {
            th.append("<th align=\"left\" style=\"padding:10px 12px;background:").append(HEAD)
                    .append(";color:#ffffff;font-size:11px;letter-spacing:1px;text-transform:uppercase\">")
                    .append(c).append("</th>");
        }

        String medico = esc(consulta.getMedico().getNombreCompleto());
        String html = "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:" + BG + ";padding:24px 0\"><tr><td align=\"center\">"
                + "<table role=\"presentation\" width=\"640\" cellpadding=\"0\" cellspacing=\"0\" style=\"width:640px;max-width:100%;background:#ffffff;border:1px solid " + LINE + ";font-family:" + FUENTE + ";color:" + TEXTO + "\">"
                // encabezado
                + "<tr><td style=\"background:" + HEAD + ";padding:22px 28px;border-bottom:4px solid " + ACENTO + "\">"
                + "<table role=\"presentation\" width=\"100%\"><tr>"
                + "<td><div style=\"font-family:" + FUENTE_TITULO + ";font-size:20px;font-weight:bold;color:#ffffff\">&#10010; Hospital " + esc(nombreHospital) + "</div>"
                + "<div style=\"font-size:11px;color:" + ACENTO + ";letter-spacing:2px;text-transform:uppercase;margin-top:3px\">Receta M&eacute;dica</div></td>"
                + "<td align=\"right\" style=\"color:#ffffff;font-size:12px;line-height:1.6\">Folio <b style=\"font-size:16px\">N.&ordm; " + String.format("%06d", receta.getId()) + "</b><br>Emitida: " + receta.getCreadoEn().format(FORMATO_FECHA) + "</td>"
                + "</tr></table></td></tr>"
                // paciente / médico
                + "<tr><td style=\"padding:24px 28px 8px\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"font-size:13px;background:" + BOX + ";border:1px solid " + LINE + "\"><tr>"
                + "<td style=\"padding:14px 16px;width:50%;vertical-align:top\"><div style=\"font-size:10px;letter-spacing:1.5px;color:" + MUTED + ";text-transform:uppercase\">Paciente</div>"
                + "<div style=\"font-size:15px;font-weight:bold;margin-top:3px\">" + esc(cita.getPaciente().getNombreCompleto()) + "</div></td>"
                + "<td style=\"padding:14px 16px;vertical-align:top\"><div style=\"font-size:10px;letter-spacing:1.5px;color:" + MUTED + ";text-transform:uppercase\">M&eacute;dico tratante</div>"
                + "<div style=\"font-size:15px;font-weight:bold;margin-top:3px\">" + medico + "</div>"
                + "<div style=\"color:" + MUTED + ";font-size:12px\">" + esc(cita.getEspecialidad().getNombre()) + "</div></td>"
                + "</tr></table></td></tr>"
                // Rx + tabla
                + "<tr><td style=\"padding:14px 28px 4px\"><span style=\"font-family:" + FUENTE_TITULO + ";font-size:34px;font-style:italic;color:" + ACENTO + ";font-weight:bold\">&#8478;</span></td></tr>"
                + "<tr><td style=\"padding:0 28px\"><table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"font-size:13px;border:1px solid " + LINE + ";border-collapse:collapse\"><tr>" + th + "</tr>" + filas + "</table></td></tr>"
                // vigencia y firma
                + "<tr><td style=\"padding:26px 28px 20px\"><table role=\"presentation\" width=\"100%\"><tr>"
                + "<td style=\"font-size:12px;color:" + MUTED + ";line-height:1.6;vertical-align:bottom;width:60%\">V&aacute;lida por <b style=\"color:" + TEXTO + "\">7 d&iacute;as</b> desde su emisi&oacute;n.<br>Puede adquirir sus medicamentos en la farmacia de la cl&iacute;nica.</td>"
                + "<td align=\"center\" style=\"vertical-align:bottom\"><div style=\"border-top:1px solid " + TEXTO + ";padding-top:6px;font-size:12px\"><b>" + medico + "</b><br><span style=\"color:" + MUTED + "\">Firma y sello</span></div></td>"
                + "</tr></table></td></tr>"
                // pie
                + "<tr><td align=\"center\" style=\"background:" + BOX + ";border-top:1px solid " + LINE + ";padding:16px;font-size:11px;color:" + MUTED + ";line-height:1.6\">" + esc(piePagina()) + "</td></tr>"
                + "</table></td></tr></table>";

        enviarHtml(cita.getPaciente().getCorreoElectronico(),
                "Receta Médica #" + receta.getId() + " - Hospital " + nombreHospital, html);
    }

    // Comprobante de compra/despacho en farmacia
    public void enviarComprobanteFarmacia(Usuario paciente, Pago pago, List<String> lineasItems) {
        if (paciente == null) return; // venta libre sin paciente identificado
        List<String[]> filas = new ArrayList<>();
        for (String linea : lineasItems) {
            filas.add(fila("Medicamento", linea.replaceFirst("^- ", ""), false));
        }
        filas.addAll(filasPago(pago));
        enviarPlantilla(paciente.getCorreoElectronico(),
                "Comprobante de Farmacia - Hospital " + nombreHospital,
                "Comprobante de farmacia",
                "Estimado(a) <b>" + esc(paciente.getNombreCompleto()) + "</b>, este es el detalle de su compra en farmacia.",
                filas, null);
    }

    // =====================================================================
    // PLANTILLA (Opción C · Elegante Sobrio)
    // =====================================================================

    private static final String BG = "#F4F3F0";
    private static final String HEAD = "#1B1B1F";
    private static final String ACENTO = "#B8924A";
    private static final String TEXTO = "#222222";
    private static final String MUTED = "#7A7770";
    private static final String LINE = "#E4E0D8";
    private static final String BOX = "#FAF8F4";
    private static final String FUENTE = "Helvetica,Arial,sans-serif";
    private static final String FUENTE_TITULO = "Georgia,'Times New Roman',serif";

    private static String[] fila(String etiqueta, String valor, boolean destacada) {
        return new String[]{etiqueta, valor, destacada ? "1" : "0"};
    }

    private List<String[]> filasCita(Cita cita) {
        List<String[]> f = new ArrayList<>();
        f.add(fila("No. de cita", "#" + cita.getId(), false));
        f.add(fila("Médico", cita.getMedico().getNombreCompleto(), false));
        f.add(fila("Especialidad", cita.getEspecialidad().getNombre(), false));
        f.add(fila("Sucursal", cita.getSucursal().getNombre(), false));
        f.add(fila("Fecha y hora", cita.getFechaHora().format(FORMATO_FECHA), true));
        if (cita.getMotivoConsulta() != null && !cita.getMotivoConsulta().isBlank()) {
            f.add(fila("Motivo", cita.getMotivoConsulta(), false));
        }
        return f;
    }

    private List<String[]> filasPago(Pago pago) {
        List<String[]> f = new ArrayList<>();
        f.add(fila("No. de transacción", pago.getNumeroTransaccion(), true));
        f.add(fila("Monto pagado", "Q" + pago.getMonto(), false));
        f.add(fila("Forma de pago", pago.getMetodoPago(), false));
        f.add(fila("Fecha del pago", pago.getCreadoEn().format(FORMATO_FECHA), false));
        return f;
    }

    // Correo estándar: encabezado oscuro con filete dorado, título serif, caja de detalle y pie.
    // 'introHtml' ya viene con sus valores escapados; filas y nota se escapan aquí.
    private void enviarPlantilla(String destinatario, String asunto, String titulo, String introHtml,
                                 List<String[]> filas, String nota) {
        StringBuilder detalle = new StringBuilder();
        if (filas != null && !filas.isEmpty()) {
            detalle.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin:22px 0;background:")
                    .append(BOX).append(";border:1px solid ").append(LINE).append(";border-left:4px solid ").append(ACENTO)
                    .append("\"><tr><td style=\"padding:18px 20px\"><table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"font-size:14px\">");
            for (String[] f : filas) {
                boolean dest = "1".equals(f[2]);
                detalle.append("<tr><td style=\"color:").append(MUTED).append(";padding:5px 12px 5px 0;width:38%;vertical-align:top\">")
                        .append(esc(f[0])).append("</td><td style=\"padding:5px 0;")
                        .append(dest ? "font-weight:bold;color:" + HEAD + ";" : "")
                        .append("\">").append(esc(f[1])).append("</td></tr>");
            }
            detalle.append("</table></td></tr></table>");
        }

        String html = "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:" + BG + ";padding:24px 0\"><tr><td align=\"center\">"
                + "<table role=\"presentation\" width=\"600\" cellpadding=\"0\" cellspacing=\"0\" style=\"width:600px;max-width:100%;background:#ffffff;border:1px solid " + LINE + ";font-family:" + FUENTE + ";color:" + TEXTO + "\">"
                + "<tr><td align=\"left\" style=\"background:" + HEAD + ";padding:26px 32px;border-bottom:4px solid " + ACENTO + "\">"
                + "<div style=\"font-family:" + FUENTE_TITULO + ";font-size:22px;font-weight:bold;color:#ffffff;letter-spacing:.5px\">&#10010; Hospital " + esc(nombreHospital) + "</div>"
                + "<div style=\"font-size:12px;color:" + ACENTO + ";letter-spacing:2px;text-transform:uppercase;margin-top:4px\">Sistema Inform&aacute;tico Hospitalario</div></td></tr>"
                + "<tr><td style=\"padding:32px\">"
                + "<div style=\"font-family:" + FUENTE_TITULO + ";font-size:21px;font-weight:bold;color:" + HEAD + ";margin-bottom:6px\">" + esc(titulo) + "</div>"
                + "<div style=\"font-size:15px;line-height:1.6;color:" + TEXTO + "\">" + introHtml + "</div>"
                + detalle
                + (nota != null && !nota.isBlank()
                ? "<div style=\"font-size:13px;color:" + MUTED + ";line-height:1.6\">" + esc(nota) + "</div>" : "")
                + "</td></tr>"
                + "<tr><td align=\"center\" style=\"background:" + BOX + ";border-top:1px solid " + LINE + ";padding:18px 32px;font-size:11px;line-height:1.6;color:" + MUTED + "\">"
                + "Correo autom&aacute;tico del Sistema Inform&aacute;tico Hospitalario, no responda a este mensaje.<br>Consultas: " + esc(telefonoHospital) + "</td></tr>"
                + "</table></td></tr></table>";

        enviarHtml(destinatario, asunto, html);
    }

    private static String tdReceta(String valor, boolean negrita) {
        return "<td style=\"padding:10px 12px;border-bottom:1px solid " + LINE + ";vertical-align:top"
                + (negrita ? ";font-weight:bold" : "") + "\">" + esc(valor) + "</td>";
    }

    // =====================================================================
    // UTILIDADES
    // =====================================================================

    private String piePagina() {
        return "Correo automático del Sistema Informático Hospitalario, no responda a este mensaje. "
                + "Consultas: " + telefonoHospital;
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
                .replace("\n", "<br>");
    }

    private void enviarHtml(String destinatario, String asunto, String html) {
        programar(destinatario, () -> {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, false, "UTF-8");
            helper.setFrom(remitente);
            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText(html, true);
            mailSender.send(mime);
        });
    }

    private interface Envio {
        void ejecutar() throws Exception;
    }

    // El contenido ya fue construido (con las entidades cargadas) antes de llegar aquí.
    // El envío ocurre DESPUÉS del commit (si la transacción falla no sale correo) y en otro hilo.
    private void programar(String destinatario, Envio envio) {
        if (destinatario == null || destinatario.isBlank()) {
            log.warn("No se envió correo: el destinatario no tiene correo electrónico.");
            return;
        }

        Runnable tarea = () -> {
            try {
                envio.ejecutar();
            } catch (Exception e) {
                // RN-GLOBAL-006: si falla el envío se registra pero NO se propaga
                Throwable raiz = e;
                while (raiz.getCause() != null && raiz.getCause() != raiz) {
                    raiz = raiz.getCause();
                }
                log.error("Error al enviar correo a {}. Causa raíz: {}: {}",
                        destinatario, raiz.getClass().getSimpleName(), raiz.getMessage());
            }
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    ejecutor.submit(tarea);
                }
            });
        } else {
            ejecutor.submit(tarea);
        }
    }
}