package com.example.demo.service;

import com.example.demo.modelo.Cita;
import com.example.demo.modelo.CitaSeguimiento;
import com.example.demo.modelo.Pago;
import com.example.demo.modelo.Usuario;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String remitente;

    @Value("${app.hospital.nombre}")
    private String nombreHospital;

    @Value("${app.hospital.telefono}")
    private String telefonoHospital;

    // Correo de bienvenida al registrar un paciente (CU-02, RN-GLOBAL-006)
    public void enviarBienvenida(Usuario paciente) {
        String asunto = "Bienvenido al Sistema de Citas - Hospital " + nombreHospital;
        String cuerpo = "Estimado(a) " + paciente.getNombreCompleto() + ",\n\n"
                + "Su registro ha sido completado exitosamente. Ya puede agendar sus citas médicas "
                + "a través de nuestro portal con su usuario: " + paciente.getNombreUsuario() + ".\n\n"
                + piePagina();

        enviar(paciente.getCorreoElectronico(), asunto, cuerpo);
    }

    // Comprobante de pago en línea, confirma la cita (CU-04, RN-CU04-05 / RN-GLOBAL-006)
    public void enviarComprobantePago(Cita cita, Pago pago) {
        String asunto = "Comprobante de Pago - Cita Médica - Hospital " + nombreHospital;
        String cuerpo = "Estimado(a) " + cita.getPaciente().getNombreCompleto() + ",\n\n"
                + "Su cita ha sido confirmada. Detalle del comprobante:\n\n"
                + "No. de transacción: " + pago.getNumeroTransaccion() + "\n"
                + "Monto pagado: Q" + pago.getMonto() + "\n"
                + "Forma de pago: " + pago.getMetodoPago() + "\n"
                + "Fecha y hora: " + pago.getCreadoEn().format(FORMATO_FECHA) + "\n\n"
                + "Detalle de la cita:\n"
                + "Médico: " + cita.getMedico().getNombreCompleto() + "\n"
                + "Especialidad: " + cita.getEspecialidad().getNombre() + "\n"
                + "Sucursal: " + cita.getSucursal().getNombre() + "\n"
                + "Fecha de la cita: " + cita.getFechaHora().format(FORMATO_FECHA) + "\n\n"
                + piePagina();

        enviar(cita.getPaciente().getCorreoElectronico(), asunto, cuerpo);
    }

    // Notificación al agendar una cita de seguimiento (CU-12, RN-CU11-04)
    public void enviarSeguimientoAgendado(Cita citaSeguimiento, CitaSeguimiento seguimiento) {
        String asunto = "Cita de Seguimiento Agendada - Hospital " + nombreHospital;
        String tipoLegible = CitaSeguimiento.MONITOREO_TRATAMIENTO.equals(seguimiento.getTipoSeguimiento())
                ? "Monitoreo de Tratamiento" : "Revisión de Resultados de Laboratorio";

        String cuerpo = "Estimado(a) " + citaSeguimiento.getPaciente().getNombreCompleto() + ",\n\n"
                + "Se ha agendado una cita de seguimiento con los siguientes datos:\n\n"
                + "Tipo de seguimiento: " + tipoLegible + "\n"
                + "Médico: " + citaSeguimiento.getMedico().getNombreCompleto() + "\n"
                + "Sucursal: " + citaSeguimiento.getSucursal().getNombre() + "\n"
                + "Fecha y hora: " + citaSeguimiento.getFechaHora().format(FORMATO_FECHA) + "\n"
                + "Observaciones: " + seguimiento.getMotivoSeguimiento() + "\n\n"
                + piePagina();

        enviar(citaSeguimiento.getPaciente().getCorreoElectronico(), asunto, cuerpo);
    }

    // Recordatorio 1-2 días antes de la cita de seguimiento (CU-12, RN-CU11-05)
    public void enviarRecordatorioSeguimiento(Cita citaSeguimiento) {
        String asunto = "Recordatorio: Su Cita de Seguimiento Mañana";
        String cuerpo = "Estimado(a) " + citaSeguimiento.getPaciente().getNombreCompleto() + ",\n\n"
                + "Le recordamos su próxima cita de seguimiento:\n\n"
                + "Médico: " + citaSeguimiento.getMedico().getNombreCompleto() + "\n"
                + "Especialidad: " + citaSeguimiento.getEspecialidad().getNombre() + "\n"
                + "Sucursal: " + citaSeguimiento.getSucursal().getNombre() + "\n"
                + "Fecha y hora: " + citaSeguimiento.getFechaHora().format(FORMATO_FECHA) + "\n\n"
                + piePagina();

        enviar(citaSeguimiento.getPaciente().getCorreoElectronico(), asunto, cuerpo);
    }

    private String piePagina() {
        return "Este es un correo automático del Sistema Informático Hospitalario. No responda a este mensaje. "
                + "Para consultas, comuníquese al teléfono " + telefonoHospital + ".";
    }

    private void enviar(String destinatario, String asunto, String cuerpo) {
        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom(remitente);
            mensaje.setTo(destinatario);
            mensaje.setSubject(asunto);
            mensaje.setText(cuerpo);
            mailSender.send(mensaje);
        } catch (Exception e) {
            // RN-GLOBAL-006: si falla el envío, se registra pero NO se propaga
            // (el flujo de negocio -pago/registro- ya se completó igual)
            Throwable raiz = e;
            while (raiz.getCause() != null && raiz.getCause() != raiz) {
                raiz = raiz.getCause();
            }
            log.error("Error al enviar correo a {}. Causa raíz: {}: {}",
                    destinatario, raiz.getClass().getSimpleName(), raiz.getMessage());
        }
    }
}