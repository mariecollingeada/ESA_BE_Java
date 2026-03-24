package com.example.demo.integrationTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.demo.auth.AuthService;
import com.example.demo.auth.PasswordTokenRepository;
import com.example.demo.auth.UserRepository;
import com.example.demo.auth.dto.RegisterRequest;
import com.example.demo.auth.dto.ResetPasswordRequest;
import com.example.demo.auth.models.PasswordResetToken;
import com.example.demo.auth.models.User;
import com.example.demo.security.JwtUtil;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
class AuthIntegrationTest {

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

  @Autowired UserRepository userRepository;
  @Autowired PasswordTokenRepository passwordTokenRepository;
  @Autowired PasswordEncoder passwordEncoder;
  @Autowired AuthService authService;
  @Autowired JwtUtil jwtUtil;

  @BeforeEach
  void setUp() {
    passwordTokenRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  void save_and_findUser() {
    User user = new User();
    user.setUsername("tc");
    user.setEmail("tc@example.com");
    user.setPassword("encoded");
    user.setEnabled(true);
    userRepository.save(user);

    assertTrue(userRepository.existsByEmail("tc@example.com"));
  }

  @Test
  void register_shouldCreateUserInDatabase() {
    RegisterRequest request = new RegisterRequest();
    request.setUsername("integrationuser");
    request.setEmail("integration@example.com");
    request.setPassword("Password123!");
    request.setConfirmPassword("Password123!");

    User savedUser = authService.register(request);

    assertNotNull(savedUser.getId());
    assertTrue(userRepository.existsByEmail("integration@example.com"));
    assertTrue(userRepository.existsByUsername("integrationuser"));

    User foundUser = userRepository.findByUsername("integrationuser").orElseThrow();
    assertEquals("integration@example.com", foundUser.getEmail());
    assertTrue(foundUser.isEnabled());
    assertTrue(foundUser.getRoles().contains("ROLE_USER"));
  }

  @Test
  void register_withDuplicateEmail_shouldThrow() {
    // Create existing user
    User existingUser =
        User.builder()
            .username("existing")
            .email("duplicate@example.com")
            .password(passwordEncoder.encode("password"))
            .roles(Set.of("ROLE_USER"))
            .enabled(true)
            .build();
    userRepository.save(existingUser);

    RegisterRequest request = new RegisterRequest();
    request.setUsername("newuser");
    request.setEmail("duplicate@example.com");
    request.setPassword("Password123!");
    request.setConfirmPassword("Password123!");

    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> authService.register(request));
    assertEquals("Email is already registered", exception.getMessage());
  }

  @Test
  void register_withDuplicateUsername_shouldThrow() {
    // Create existing user
    User existingUser =
        User.builder()
            .username("duplicateuser")
            .email("existing@example.com")
            .password(passwordEncoder.encode("password"))
            .roles(Set.of("ROLE_USER"))
            .enabled(true)
            .build();
    userRepository.save(existingUser);

    RegisterRequest request = new RegisterRequest();
    request.setUsername("duplicateuser");
    request.setEmail("new@example.com");
    request.setPassword("Password123!");
    request.setConfirmPassword("Password123!");

    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> authService.register(request));
    assertEquals("Username already exists", exception.getMessage());
  }

  @Test
  void loadUserByUsername_shouldReturnUserDetails() {
    // Create user
    User user =
        User.builder()
            .username("loginuser")
            .email("login@example.com")
            .password(passwordEncoder.encode("Password123!"))
            .roles(Set.of("ROLE_USER"))
            .enabled(true)
            .build();
    userRepository.save(user);

    UserDetails userDetails = authService.loadUserByUsername("loginuser");

    assertEquals("loginuser", userDetails.getUsername());
    assertTrue(userDetails.isEnabled());
    assertTrue(
        userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
  }

  @Test
  void loadUserByUsername_withNonExistentUser_shouldThrow() {
    assertThrows(
        UsernameNotFoundException.class, () -> authService.loadUserByUsername("nonexistent"));
  }

  @Test
  void findByUsername_shouldReturnUser() {
    User user =
        User.builder()
            .username("finduser")
            .email("find@example.com")
            .password(passwordEncoder.encode("Password123!"))
            .roles(Set.of("ROLE_USER"))
            .enabled(true)
            .build();
    userRepository.save(user);

    User foundUser = authService.findByUsername("finduser");

    assertEquals("finduser", foundUser.getUsername());
    assertEquals("find@example.com", foundUser.getEmail());
  }

  @Test
  void jwtToken_shouldBeValidAndExtractCorrectUsername() {
    // Create user
    User user =
        User.builder()
            .username("jwtuser")
            .email("jwt@example.com")
            .password(passwordEncoder.encode("Password123!"))
            .roles(Set.of("ROLE_USER"))
            .enabled(true)
            .build();
    userRepository.save(user);

    String token = jwtUtil.generateToken("jwtuser");

    assertTrue(jwtUtil.validateToken(token));
    assertEquals("jwtuser", jwtUtil.extractUsername(token));
  }

  @Test
  void passwordResetFlow_shouldWorkEndToEnd() {
    // Create user
    User user =
        User.builder()
            .username("resetuser")
            .email("reset@example.com")
            .password(passwordEncoder.encode("OldPassword123!"))
            .roles(Set.of("ROLE_USER"))
            .enabled(true)
            .build();
    userRepository.save(user);

    // Create password reset token
    PasswordResetToken resetToken = authService.createPasswordResetToken(user);
    assertNotNull(resetToken.getToken());
    assertFalse(resetToken.isExpired());
    assertFalse(resetToken.isUsed());

    // Reset password
    ResetPasswordRequest request = new ResetPasswordRequest();
    request.setToken(resetToken.getToken());
    request.setNewPassword("NewPassword123!");
    request.setConfirmPassword("NewPassword123!");

    authService.resetPassword(request);

    // Verify token is marked as used
    PasswordResetToken usedToken =
        passwordTokenRepository.findByToken(resetToken.getToken()).orElseThrow();
    assertTrue(usedToken.isUsed());

    // Verify new password is encoded and different from old
    User updatedUser = userRepository.findByUsername("resetuser").orElseThrow();
    assertTrue(passwordEncoder.matches("NewPassword123!", updatedUser.getPassword()));
    assertFalse(passwordEncoder.matches("OldPassword123!", updatedUser.getPassword()));
  }

  @Test
  void resetPassword_withUsedToken_shouldThrow() {
    // Create user
    User user =
        User.builder()
            .username("usedtokenuser")
            .email("usedtoken@example.com")
            .password(passwordEncoder.encode("Password123!"))
            .roles(Set.of("ROLE_USER"))
            .enabled(true)
            .build();
    userRepository.save(user);

    // Create and mark token as used
    PasswordResetToken resetToken = authService.createPasswordResetToken(user);
    resetToken.setUsed(true);
    passwordTokenRepository.save(resetToken);

    ResetPasswordRequest request = new ResetPasswordRequest();
    request.setToken(resetToken.getToken());
    request.setNewPassword("NewPassword123!");
    request.setConfirmPassword("NewPassword123!");

    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> authService.resetPassword(request));
    assertEquals("Reset token has already been used", exception.getMessage());
  }

  @Test
  void resetPassword_withInvalidToken_shouldThrow() {
    ResetPasswordRequest request = new ResetPasswordRequest();
    request.setToken("invalid-token-that-does-not-exist");
    request.setNewPassword("NewPassword123!");
    request.setConfirmPassword("NewPassword123!");

    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> authService.resetPassword(request));
    assertEquals("Invalid reset token", exception.getMessage());
  }

  @Test
  void register_shouldNormalizeEmailAndUsername() {
    RegisterRequest request = new RegisterRequest();
    request.setUsername("  CASEUSER  ");
    request.setEmail("  UPPERCASE@EXAMPLE.COM  ");
    request.setPassword("Password123!");
    request.setConfirmPassword("Password123!");

    authService.register(request);

    assertTrue(userRepository.existsByEmail("uppercase@example.com"));
    assertTrue(userRepository.existsByUsername("caseuser"));
    User savedUser = userRepository.findByEmail("uppercase@example.com").orElseThrow();
    assertEquals("uppercase@example.com", savedUser.getEmail());
    assertEquals("caseuser", savedUser.getUsername());
  }

  @Test
  void register_withPasswordMismatch_shouldThrow() {
    RegisterRequest request = new RegisterRequest();
    request.setUsername("mismatchuser");
    request.setEmail("mismatch@example.com");
    request.setPassword("Password123!");
    request.setConfirmPassword("DifferentPassword!");

    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> authService.register(request));
    assertEquals("password policy not met", exception.getMessage());
  }

  @Test
  void register_withShortPassword_shouldThrow() {
    RegisterRequest request = new RegisterRequest();
    request.setUsername("shortpwuser");
    request.setEmail("shortpw@example.com");
    request.setPassword("short");
    request.setConfirmPassword("short");

    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> authService.register(request));
    assertEquals("password policy not met", exception.getMessage());
  }

  @Test
  void resetPassword_withPasswordMismatch_shouldThrow() {
    // Create user
    User user =
        User.builder()
            .username("pwmismatchuser")
            .email("pwmismatch@example.com")
            .password(passwordEncoder.encode("Password123!"))
            .roles(Set.of("ROLE_USER"))
            .enabled(true)
            .build();
    userRepository.save(user);

    PasswordResetToken resetToken = authService.createPasswordResetToken(user);

    ResetPasswordRequest request = new ResetPasswordRequest();
    request.setToken(resetToken.getToken());
    request.setNewPassword("NewPassword123!");
    request.setConfirmPassword("DifferentPassword!");

    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> authService.resetPassword(request));
    assertEquals("Password policy not met", exception.getMessage());
  }

  @Test
  void resetPassword_withShortPassword_shouldThrow() {
    // Create user
    User user =
        User.builder()
            .username("shortresetuser")
            .email("shortreset@example.com")
            .password(passwordEncoder.encode("Password123!"))
            .roles(Set.of("ROLE_USER"))
            .enabled(true)
            .build();
    userRepository.save(user);

    PasswordResetToken resetToken = authService.createPasswordResetToken(user);

    ResetPasswordRequest request = new ResetPasswordRequest();
    request.setToken(resetToken.getToken());
    request.setNewPassword("short");
    request.setConfirmPassword("short");

    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> authService.resetPassword(request));
    assertEquals("Password policy not met", exception.getMessage());
  }
}
