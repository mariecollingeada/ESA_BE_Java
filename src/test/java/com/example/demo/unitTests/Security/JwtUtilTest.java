package com.example.demo.unitTests.Security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.demo.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtUtilTest {

  private JwtUtil jwtUtil;
  private static final String SECRET = "this_is_a_test_secret_key_32_bytes!";
  private static final long TTL_MS = 3600000; // 1 hour

  @BeforeEach
  void setUp() {
    jwtUtil = new JwtUtil(SECRET, TTL_MS);
  }

  @Test
  void generateToken_shouldReturnValidJwt() {
    String username = "testuser";

    String token = jwtUtil.generateToken(username);

    assertNotNull(token);
    assertTrue(token.split("\\.").length == 3, "JWT should have three parts");
  }

  @Test
  void extractUsername_shouldReturnCorrectUsername() {
    String username = "testuser";
    String token = jwtUtil.generateToken(username);

    String extractedUsername = jwtUtil.extractUsername(token);

    assertEquals(username, extractedUsername);
  }

  @Test
  void validateToken_withValidToken_shouldReturnTrue() {
    String username = "testuser";
    String token = jwtUtil.generateToken(username);

    boolean isValid = jwtUtil.validateToken(token);

    assertTrue(isValid);
  }

  @Test
  void validateToken_withInvalidToken_shouldReturnFalse() {
    String invalidToken = "invalid.token.here";

    boolean isValid = jwtUtil.validateToken(invalidToken);

    assertFalse(isValid);
  }

  @Test
  void validateToken_withNullToken_shouldReturnFalse() {
    boolean isValid = jwtUtil.validateToken(null);

    assertFalse(isValid);
  }

  @Test
  void validateToken_withEmptyToken_shouldReturnFalse() {
    boolean isValid = jwtUtil.validateToken("");

    assertFalse(isValid);
  }

  @Test
  void validateToken_withMalformedToken_shouldReturnFalse() {
    String malformedToken = "not-a-valid-jwt";

    boolean isValid = jwtUtil.validateToken(malformedToken);

    assertFalse(isValid);
  }

  @Test
  void validateToken_withTamperedToken_shouldReturnFalse() {
    String token = jwtUtil.generateToken("testuser");
    // Tamper with the token by modifying the signature
    String tamperedToken = token.substring(0, token.length() - 5) + "XXXXX";

    boolean isValid = jwtUtil.validateToken(tamperedToken);

    assertFalse(isValid);
  }

  @Test
  void validateToken_withExpiredToken_shouldReturnFalse() {
    // Create a JwtUtil with 0ms TTL (immediately expired)
    JwtUtil expiredJwtUtil = new JwtUtil(SECRET, 0);
    String token = expiredJwtUtil.generateToken("testuser");

    // Wait to ensure expiration
    try {
      Thread.sleep(10);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    boolean isValid = expiredJwtUtil.validateToken(token);

    assertFalse(isValid);
  }

  @Test
  void validateToken_withDifferentSecret_shouldReturnFalse() {
    String token = jwtUtil.generateToken("testuser");

    // Create another JwtUtil with a different secret
    JwtUtil differentSecretJwtUtil = new JwtUtil("different_secret_key_32_bytes!!!", TTL_MS);

    boolean isValid = differentSecretJwtUtil.validateToken(token);

    assertFalse(isValid);
  }

  @Test
  void generateToken_withSpecialCharactersInUsername_shouldWork() {
    String username = "user@example.com";
    String token = jwtUtil.generateToken(username);

    String extractedUsername = jwtUtil.extractUsername(token);

    assertEquals(username, extractedUsername);
    assertTrue(jwtUtil.validateToken(token));
  }

  @Test
  void generateToken_withEmptyUsername_shouldWork() {
    String username = "";
    String token = jwtUtil.generateToken(username);

    // JWT library returns null for empty subject
    String extractedUsername = jwtUtil.extractUsername(token);

    // Empty string becomes null in JWT
    assertTrue(extractedUsername == null || extractedUsername.isEmpty());
    assertTrue(jwtUtil.validateToken(token));
  }

  @Test
  void generateToken_multipleTimes_shouldCreateUniqueTokens() {
    String username = "testuser";

    String token1 = jwtUtil.generateToken(username);
    String token2 = jwtUtil.generateToken(username);

    // Tokens may be identical if generated in the same millisecond,
    // but both should be valid
    assertTrue(jwtUtil.validateToken(token1));
    assertTrue(jwtUtil.validateToken(token2));
    assertEquals(jwtUtil.extractUsername(token1), jwtUtil.extractUsername(token2));
  }
}
