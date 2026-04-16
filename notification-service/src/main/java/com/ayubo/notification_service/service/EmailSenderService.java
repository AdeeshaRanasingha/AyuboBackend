package com.ayubo.notification_service.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Year;

@Service
@RequiredArgsConstructor
public class EmailSenderService {

    private static final Logger log = LoggerFactory.getLogger(EmailSenderService.class);

    private final JavaMailSender mailSender;

    @Value("${notification.email.enabled:true}")
    private boolean emailEnabled;

    @Value("${notification.email.from:}")
    private String fromEmail;

    // ─────────────────────────────────────────────────────────────────────────
    //  Public API — same signature as before; callers need NO changes.
    // ─────────────────────────────────────────────────────────────────────────
    public boolean sendEmail(String to, String subject, String message) {
        if (!emailEnabled || !StringUtils.hasText(to)) {
            return false;
        }

        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            if (StringUtils.hasText(fromEmail)) {
                helper.setFrom(fromEmail, "Ayubo Health");
            }

            helper.setText(buildHtmlEmail(subject, message, to), true);   // true = isHtml

            mailSender.send(mime);
            return true;
        } catch (MessagingException | java.io.UnsupportedEncodingException ex) {
            log.error("Email send failed", ex);
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HTML email template — professional, branded, real-world layout
    // ─────────────────────────────────────────────────────────────────────────
    private String buildHtmlEmail(String subject, String message, String recipientEmail) {
        int currentYear = Year.now().getValue();

        // Resolve a friendly icon based on the subject keyword
        String icon = "📋";
        String accentColor = "#0F9488";   // teal default
        String subjectLower = subject == null ? "" : subject.toLowerCase();
        if (subjectLower.contains("created") || subjectLower.contains("scheduled")) {
            icon = "✅"; accentColor = "#059669";   // green
        } else if (subjectLower.contains("cancelled") || subjectLower.contains("canceled")) {
            icon = "❌"; accentColor = "#DC2626";   // red
        } else if (subjectLower.contains("updated") || subjectLower.contains("rescheduled")) {
            icon = "🔄"; accentColor = "#D97706";   // amber
        } else if (subjectLower.contains("reminder")) {
            icon = "⏰"; accentColor = "#2563EB";   // blue
        } else if (subjectLower.contains("payment")) {
            icon = "💳"; accentColor = "#7C3AED";   // violet
        }

        return "<!DOCTYPE html>\n" +
            "<html lang=\"en\">\n" +
            "<head>\n" +
            "  <meta charset=\"UTF-8\" />\n" +
            "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\" />\n" +
            "  <title>" + escapeHtml(subject) + "</title>\n" +
            "  <!--[if mso]><noscript><xml><o:OfficeDocumentSettings><o:PixelsPerInch>96</o:PixelsPerInch></o:OfficeDocumentSettings></xml></noscript><![endif]-->\n" +
            "</head>\n" +
            "<body style=\"margin:0;padding:0;background-color:#f1f5f9;font-family:'Segoe UI',Arial,sans-serif;\">\n" +
            "\n" +
            "  <!-- Outer wrapper -->\n" +
            "  <table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"background-color:#f1f5f9;padding:40px 16px;\">\n" +
            "    <tr><td align=\"center\">\n" +
            "\n" +
            "      <!-- Email card -->\n" +
            "      <table width=\"600\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"max-width:600px;width:100%;background:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 4px 24px rgba(15,30,46,0.10);\">\n" +
            "\n" +
            "        <!-- ── Header bar ── -->\n" +
            "        <tr>\n" +
            "          <td style=\"background:linear-gradient(135deg,#0A1624 0%,#102235 60%,#0A1624 100%);padding:36px 40px 28px;\">\n" +
            "            <!-- Logo row -->\n" +
            "            <table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">\n" +
            "              <tr>\n" +
            "                <td style=\"vertical-align:middle;\">\n" +
            "                  <!-- Wordmark -->\n" +
            "                  <span style=\"font-size:26px;font-weight:800;letter-spacing:-0.5px;color:#ffffff;\">&#9651; Ayubo</span>\n" +
            "                  <span style=\"font-size:12px;font-weight:500;color:#2DD4BF;margin-left:6px;letter-spacing:0.08em;\">HEALTH</span>\n" +
            "                </td>\n" +
            "                <td align=\"right\" style=\"vertical-align:middle;\">\n" +
            "                  <span style=\"font-size:11px;color:#94a3b8;letter-spacing:0.06em;\">Notification</span>\n" +
            "                </td>\n" +
            "              </tr>\n" +
            "            </table>\n" +
            "\n" +
            "            <!-- Divider -->\n" +
            "            <div style=\"height:1px;background:rgba(255,255,255,0.08);margin:20px 0;\"></div>\n" +
            "\n" +
            "            <!-- Subject + icon -->\n" +
            "            <p style=\"margin:0 0 6px;font-size:13px;font-weight:600;letter-spacing:0.14em;text-transform:uppercase;color:" + accentColor + ";\">" + icon + "&nbsp;&nbsp;" + escapeHtml(subject) + "</p>\n" +
            "          </td>\n" +
            "        </tr>\n" +
            "\n" +
            "        <!-- ── Body ── -->\n" +
            "        <tr>\n" +
            "          <td style=\"padding:40px 40px 32px;\">\n" +
            "\n" +
            "            <!-- Accent top-border -->\n" +
            "            <div style=\"height:3px;border-radius:2px;background:linear-gradient(90deg," + accentColor + ",#2DD4BF,#5eead4);margin-bottom:32px;\"></div>\n" +
            "\n" +
            "            <!-- Greeting / message -->\n" +
            "            <p style=\"margin:0 0 20px;font-size:16px;line-height:1.7;color:#1e293b;\">" + escapeHtml(message) + "</p>\n" +
            "\n" +
            "            <!-- Info box -->\n" +
            "            <table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"background:#f8fafc;border-left:3px solid " + accentColor + ";border-radius:0 8px 8px 0;margin:24px 0;\">\n" +
            "              <tr><td style=\"padding:16px 20px;\">\n" +
            "                <p style=\"margin:0 0 4px;font-size:11px;font-weight:700;letter-spacing:0.14em;text-transform:uppercase;color:#64748b;\">Sent to</p>\n" +
            "                <p style=\"margin:0;font-size:14px;font-weight:600;color:#0f172a;\">" + escapeHtml(recipientEmail) + "</p>\n" +
            "              </td></tr>\n" +
            "            </table>\n" +
            "\n" +
            "            <!-- CTA button -->\n" +
            "            <table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"margin:28px 0 0;\">\n" +
            "              <tr>\n" +
            "                <td style=\"border-radius:10px;background:" + accentColor + ";\">\n" +
            "                  <a href=\"http://localhost:5173/notifications\"\n" +
            "                     style=\"display:inline-block;padding:13px 28px;font-size:13px;font-weight:700;letter-spacing:0.1em;text-transform:uppercase;color:#ffffff;text-decoration:none;border-radius:10px;\">\n" +
            "                    View Notifications &rarr;\n" +
            "                  </a>\n" +
            "                </td>\n" +
            "              </tr>\n" +
            "            </table>\n" +
            "\n" +
            "          </td>\n" +
            "        </tr>\n" +
            "\n" +
            "        <!-- ── Footer ── -->\n" +
            "        <tr>\n" +
            "          <td style=\"background:#f8fafc;border-top:1px solid #e2e8f0;padding:24px 40px;\">\n" +
            "            <table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">\n" +
            "              <tr>\n" +
            "                <td style=\"vertical-align:top;\">\n" +
            "                  <p style=\"margin:0 0 4px;font-size:13px;font-weight:700;color:#0A1624;\">&#9651; Ayubo Health</p>\n" +
            "                  <p style=\"margin:0;font-size:11px;color:#94a3b8;\">Your trusted digital health companion</p>\n" +
            "                </td>\n" +
            "                <td align=\"right\" style=\"vertical-align:top;\">\n" +
            "                  <p style=\"margin:0;font-size:11px;color:#94a3b8;\">&copy;&nbsp;" + currentYear + "&nbsp;Ayubo Health. All rights reserved.</p>\n" +
            "                  <p style=\"margin:4px 0 0;font-size:11px;color:#94a3b8;\">This is an automated message &mdash; please do not reply.</p>\n" +
            "                </td>\n" +
            "              </tr>\n" +
            "            </table>\n" +
            "          </td>\n" +
            "        </tr>\n" +
            "\n" +
            "      </table>\n" +
            "      <!-- /card -->\n" +
            "\n" +
            "    </td></tr>\n" +
            "  </table>\n" +
            "\n" +
            "</body>\n" +
            "</html>\n";
    }

    /** Minimal HTML-escape to prevent XSS in the message/subject. */
    private String escapeHtml(String input) {
        if (input == null) return "";
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
