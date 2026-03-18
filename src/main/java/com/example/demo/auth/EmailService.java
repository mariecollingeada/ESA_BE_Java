package com.example.demo.auth;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

  @Value("${sendgrid.api-key:}")
  private String sendgridApiKey;

  @Value("${sendgrid.from-email:}")
  private String fromEmail;

  @Value("${FRONTEND_URL:http://localhost:5173}")
  private String frontEndUrl;

  public void sendPasswordResetEmail(String email, String resetToken) throws IOException {
    if (sendgridApiKey == null || sendgridApiKey.isBlank()) {
      throw new IllegalStateException("SENDGRID_API_KEY is not configured");
    }

    String resetLink = frontEndUrl + "/reset-password?token=" + resetToken;

    Email from = new Email(fromEmail);
    String subject = "Reset your password";
    Email to = new Email(email);
    Content content =
        new Content(
            "text/html",
            "<p>You requested a password reset.</p>"
                + "<p>Click the link below to set a new password:</p>"
                + "<p><a href=\""
                + resetLink
                + "\">Reset Password</a></p>"
                + "<p>This link will expire in 24 hours.</p>"
                + "<p>If you did not request this, please ignore this email.</p>");
    Mail mail = new Mail(from, subject, to, content);

    SendGrid sg = new SendGrid(sendgridApiKey);
    Request request = new Request();
    request.setMethod(Method.POST);
    request.setEndpoint("mail/send");
    request.setBody(mail.build());
    Response response = sg.api(request);
    log.info("SendGrid status: {}", response.getStatusCode());
    if (response.getStatusCode() >= 400) {
      log.error("SendGrid error: {} {}", response.getStatusCode(), response.getBody());
      throw new IOException("Failed to send email, status: " + response.getStatusCode());
    }
  }
}
