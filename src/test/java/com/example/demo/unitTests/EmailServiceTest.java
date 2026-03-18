package com.example.demo.unitTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.demo.auth.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class EmailServiceTest {

  private EmailService emailService;

  @BeforeEach
  void setUp() {
    emailService = new EmailService();
  }

  @Test
  void sendPasswordResetEmail_whenApiKeyIsBlank_shouldThrowIllegalState() {
    ReflectionTestUtils.setField(emailService, "sendgridApiKey", "");
    ReflectionTestUtils.setField(emailService, "fromEmail", "test@example.com");
    ReflectionTestUtils.setField(emailService, "frontEndUrl", "http://localhost:5173");

    IllegalStateException ex =
        assertThrows(
            IllegalStateException.class,
            () -> emailService.sendPasswordResetEmail("user@example.com", "some-token"));

    assertEquals("SENDGRID_API_KEY is not configured", ex.getMessage());
  }

  @Test
  void sendPasswordResetEmail_whenApiKeyIsNull_shouldThrowIllegalState() {
    ReflectionTestUtils.setField(emailService, "sendgridApiKey", null);
    ReflectionTestUtils.setField(emailService, "fromEmail", "test@example.com");
    ReflectionTestUtils.setField(emailService, "frontEndUrl", "http://localhost:5173");

    IllegalStateException ex =
        assertThrows(
            IllegalStateException.class,
            () -> emailService.sendPasswordResetEmail("user@example.com", "some-token"));

    assertEquals("SENDGRID_API_KEY is not configured", ex.getMessage());
  }

  @Test
  void sendPasswordResetEmail_withInvalidApiKey_shouldThrowIOException() {
    ReflectionTestUtils.setField(emailService, "sendgridApiKey", "SG.invalid-key");
    ReflectionTestUtils.setField(emailService, "fromEmail", "test@example.com");
    ReflectionTestUtils.setField(emailService, "frontEndUrl", "http://localhost:5173");

    // SendGrid will return a 401/403 for an invalid key, which we convert to IOException
    Exception ex =
        assertThrows(
            Exception.class,
            () -> emailService.sendPasswordResetEmail("user@example.com", "some-token"));

    assertTrue(
        ex.getMessage().contains("Failed to send email") || ex.getMessage().contains("status"),
        "Expected an error about failed email send, got: " + ex.getMessage());
  }
}
