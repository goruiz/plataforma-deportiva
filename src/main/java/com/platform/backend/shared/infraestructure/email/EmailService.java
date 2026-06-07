package com.platform.backend.shared.infraestructure.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from}")
    private String fromEmail;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    public void sendTeamInvitation(String to, String playerName, String teamName) {
        sendHtmlEmail(to,
                "Invitación al equipo " + teamName,
                buildTeamInvitationHtml(playerName, teamName));
    }

    public void sendRegistrationInvitation(String to, String teamName, String token) {
        String registrationUrl = frontendUrl + "/register?inviteToken=" + token;
        sendHtmlEmail(to,
                "Únete a " + teamName + " en la Plataforma Deportiva",
                buildRegistrationInvitationHtml(teamName, registrationUrl));
    }

    private void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Error al enviar el correo a " + to, e);
        }
    }

    private String buildTeamInvitationHtml(String playerName, String teamName) {
        return """
                <html>
                <body style="font-family: Arial, sans-serif; color: #333;">
                  <h2>¡Hola, %s!</h2>
                  <p>Has sido invitado a unirte al equipo <strong>%s</strong>.</p>
                  <p>Inicia sesión en la plataforma para aceptar la invitación.</p>
                  <br/>
                  <p>Plataforma Deportiva</p>
                </body>
                </html>
                """.formatted(playerName, teamName);
    }

    private String buildRegistrationInvitationHtml(String teamName, String registrationUrl) {
        return """
                <html>
                <body style="font-family: Arial, sans-serif; color: #333;">
                  <h2>¡Te han invitado a la Plataforma Deportiva!</h2>
                  <p>Has sido invitado a unirte al equipo <strong>%s</strong>.</p>
                  <p>Completa tu registro haciendo clic en el botón:</p>
                  <a href="%s"
                     style="display:inline-block;padding:12px 24px;background:#1976D2;color:#fff;
                            text-decoration:none;border-radius:4px;font-weight:bold;">
                    Registrarme
                  </a>
                  <br/><br/>
                  <p>Si el botón no funciona, copia este enlace en tu navegador:</p>
                  <p>%s</p>
                  <br/>
                  <p>Plataforma Deportiva</p>
                </body>
                </html>
                """.formatted(teamName, registrationUrl, registrationUrl);
    }
}
