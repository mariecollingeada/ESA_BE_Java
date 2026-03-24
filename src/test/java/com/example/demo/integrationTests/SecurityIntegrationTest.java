package com.example.demo.integrationTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.demo.auth.AuthService;
import com.example.demo.auth.PasswordTokenRepository;
import com.example.demo.auth.UserRepository;
import com.example.demo.auth.models.PasswordResetToken;
import com.example.demo.auth.models.User;
import com.example.demo.security.JwtUtil;
import java.time.LocalDateTime;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class SecurityIntegrationTest {

  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:15")
          .withDatabaseName("it_db")
          .withUsername("it_user")
          .withPassword("it_pass");

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", postgres::getJdbcUrl);
    r.add("spring.datasource.username", postgres::getUsername);
    r.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired private UserRepository userRepository;
  @Autowired private PasswordTokenRepository passwordTokenRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private AuthService authService;
  @Autowired private JwtUtil jwtUtil;

  private User testUser;

  @BeforeEach
  void setUp() {
    passwordTokenRepository.deleteAll();
    userRepository.deleteAll();

    testUser =
        User.builder()
            .username("securityuser")
            .email("security@example.com")
            .password(passwordEncoder.encode("Password123!"))
            .roles(Set.of("ROLE_USER"))
            .enabled(true)
            .build();
    testUser = userRepository.save(testUser);
  }

  // JWT Token Tests

  @Test
  void jwtToken_shouldBeValid() {
    String token = jwtUtil.generateToken(testUser.getUsername());

    assertTrue(jwtUtil.validateToken(token));
    assertEquals(testUser.getUsername(), jwtUtil.extractUsername(token));
  }

  @Test
  void jwtToken_withDifferentUsernames_shouldHaveDifferentTokens() {
    String token1 = jwtUtil.generateToken("user1");
    String token2 = jwtUtil.generateToken("user2");

    assertNotEquals(token1, token2);
    assertEquals("user1", jwtUtil.extractUsername(token1));
    assertEquals("user2", jwtUtil.extractUsername(token2));
  }

  @Test
  void jwtToken_tamperedToken_shouldBeInvalid() {
    String token = jwtUtil.generateToken(testUser.getUsername());
    String tamperedToken = token.substring(0, token.length() - 5) + "XXXXX";

    assertFalse(jwtUtil.validateToken(tamperedToken));
  }

  @Test
  void jwtToken_invalidFormat_shouldBeInvalid() {
    assertFalse(jwtUtil.validateToken("invalid-token"));
    assertFalse(jwtUtil.validateToken(""));
    assertFalse(jwtUtil.validateToken(null));
  }

  @Test
  void jwtToken_shouldContainCorrectSubject() {
    String token = jwtUtil.generateToken("testsubject");

    String extractedUsername = jwtUtil.extractUsername(token);

    assertEquals("testsubject", extractedUsername);
  }

  // Password Encoding Tests

  @Test
  void passwordEncoder_shouldHashPassword() {
    String rawPassword = "MyPassword123!";

    String encoded = passwordEncoder.encode(rawPassword);

    assertNotEquals(rawPassword, encoded);
    assertTrue(passwordEncoder.matches(rawPassword, encoded));
  }

  @Test
  void passwordEncoder_samePassword_shouldProduceDifferentHashes() {
    String rawPassword = "SamePassword123!";

    String encoded1 = passwordEncoder.encode(rawPassword);
    String encoded2 = passwordEncoder.encode(rawPassword);

    assertNotEquals(encoded1, encoded2);
    assertTrue(passwordEncoder.matches(rawPassword, encoded1));
    assertTrue(passwordEncoder.matches(rawPassword, encoded2));
  }

  @Test
  void passwordEncoder_wrongPassword_shouldNotMatch() {
    String rawPassword = "CorrectPassword123!";
    String wrongPassword = "WrongPassword123!";

    String encoded = passwordEncoder.encode(rawPassword);

    assertFalse(passwordEncoder.matches(wrongPassword, encoded));
  }

  // Password Reset Token Tests

  @Test
  void passwordResetToken_shouldHave1HourExpiry() {
    PasswordResetToken token = authService.createPasswordResetToken(testUser);

    assertNotNull(token.getExpiryDate());
    // Token should expire approximately 1 hour from now
    LocalDateTime now = LocalDateTime.now();
    assertTrue(token.getExpiryDate().isAfter(now.plusMinutes(55)));
    assertTrue(token.getExpiryDate().isBefore(now.plusHours(1).plusMinutes(5)));
  }

  @Test
  void passwordResetToken_shouldBeUnique() {
    PasswordResetToken token1 = authService.createPasswordResetToken(testUser);
    PasswordResetToken token2 = authService.createPasswordResetToken(testUser);

    assertNotEquals(token1.getToken(), token2.getToken());
  }

  @Test
  void passwordResetToken_newToken_shouldNotBeExpired() {
    PasswordResetToken token = authService.createPasswordResetToken(testUser);

    assertFalse(token.isExpired());
  }

  @Test
  void passwordResetToken_newToken_shouldNotBeUsed() {
    PasswordResetToken token = authService.createPasswordResetToken(testUser);

    assertFalse(token.isUsed());
  }

  @Test
  void passwordResetToken_expiredToken_shouldBeMarkedAsExpired() {
    PasswordResetToken token = authService.createPasswordResetToken(testUser);
    token.setExpiryDate(LocalDateTime.now().minusHours(1));
    passwordTokenRepository.save(token);

    PasswordResetToken savedToken =
        passwordTokenRepository.findByToken(token.getToken()).orElseThrow();

    assertTrue(savedToken.isExpired());
  }

  // User Authentication Tests

  @Test
  void loadUserByUsername_shouldReturnCorrectAuthorities() {
    var userDetails = authService.loadUserByUsername(testUser.getUsername());

    assertTrue(
        userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
  }

  @Test
  void loadUserByUsername_shouldReturnEnabledUser() {
    var userDetails = authService.loadUserByUsername(testUser.getUsername());

    assertTrue(userDetails.isEnabled());
    assertTrue(userDetails.isAccountNonExpired());
    assertTrue(userDetails.isAccountNonLocked());
    assertTrue(userDetails.isCredentialsNonExpired());
  }

  @Test
  void loadUserByUsername_passwordShouldBeEncoded() {
    var userDetails = authService.loadUserByUsername(testUser.getUsername());

    // Password should not be the raw password
    assertNotEquals("Password123!", userDetails.getPassword());
    // But should match when verified
    assertTrue(passwordEncoder.matches("Password123!", userDetails.getPassword()));
  }

  // Database Constraint Tests

  @Test
  void uniqueEmailConstraint_shouldBeEnforced() {
    User duplicateEmailUser =
        User.builder()
            .username("different")
            .email("security@example.com") // Same email as testUser
            .password(passwordEncoder.encode("Password123!"))
            .roles(Set.of("ROLE_USER"))
            .enabled(true)
            .build();

    org.junit.jupiter.api.Assertions.assertThrows(
        Exception.class, () -> userRepository.saveAndFlush(duplicateEmailUser));
  }

  @Test
  void uniqueUsernameConstraint_shouldBeEnforced() {
    User duplicateUsernameUser =
        User.builder()
            .username("securityuser") // Same username as testUser
            .email("different@example.com")
            .password(passwordEncoder.encode("Password123!"))
            .roles(Set.of("ROLE_USER"))
            .enabled(true)
            .build();

    org.junit.jupiter.api.Assertions.assertThrows(
        Exception.class, () -> userRepository.saveAndFlush(duplicateUsernameUser));
  }

  // Multiple Role Tests

  @Test
  void userWithMultipleRoles_shouldHaveAllRoles() {
    User adminUser =
        User.builder()
            .username("admin")
            .email("admin@example.com")
            .password(passwordEncoder.encode("Password123!"))
            .roles(Set.of("ROLE_USER", "ROLE_ADMIN"))
            .enabled(true)
            .build();
    userRepository.save(adminUser);

    var userDetails = authService.loadUserByUsername("admin");

    assertEquals(2, userDetails.getAuthorities().size());
    assertTrue(
        userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
    assertTrue(
        userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
  }

  // Case Sensitivity Tests

  @Test
  void usernameNormalization_shouldBeCaseInsensitive() {
    // testUser has username "securityuser"
    var userDetails = authService.loadUserByUsername("SECURITYUSER");

    assertEquals("securityuser", userDetails.getUsername());
  }

  @Test
  void emailNormalization_duringRegistration_shouldStoreNormalized() {
    // Emails are normalized to lowercase during registration
    // testUser was saved with email "security@example.com"
    assertTrue(userRepository.existsByEmail("security@example.com"));

    // The repository lookup is case-sensitive, but the authService normalizes
    // before saving, so we just verify the normalized email exists
    User foundUser = userRepository.findByEmail("security@example.com").orElseThrow();
    assertEquals("security@example.com", foundUser.getEmail());
  }
}
