package com.example.demo.unitTests.Auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.auth.AuthService;
import com.example.demo.auth.EmailService;
import com.example.demo.auth.PasswordTokenRepository;
import com.example.demo.auth.UserRepository;
import com.example.demo.auth.dto.RegisterRequest;
import com.example.demo.auth.dto.ResetPasswordRequest;
import com.example.demo.auth.models.PasswordResetToken;
import com.example.demo.auth.models.User;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private PasswordTokenRepository passwordTokenRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private EmailService emailService;
  @InjectMocks private AuthService authService;

  @Test
  void register_shouldEncodePasswordAndSaveUser() {
    RegisterRequest req = new RegisterRequest();
    req.setUsername("alice");
    req.setEmail("alice@example.com");
    req.setPassword("Password123!");
    req.setConfirmPassword("Password123!");
    when(userRepository.existsByEmail(req.getEmail())).thenReturn(false);
    when(userRepository.existsByUsername(req.getUsername())).thenReturn(false);
    when(passwordEncoder.encode(any())).thenReturn("$2a$ bcrypt-hash");

    authService.register(req);

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(captor.capture());
    User saved = captor.getValue();
    assertEquals("alice", saved.getUsername());
    assertEquals("alice@example.com", saved.getEmail());
    assertEquals("$2a$ bcrypt-hash", saved.getPassword());
    assertTrue(saved.isEnabled());
  }

  @Test
  void register_whenEmailExists_shouldThrow() {
    RegisterRequest req = new RegisterRequest("a", "a@a.com", "pw", "pw");
    when(userRepository.existsByEmail(req.getEmail())).thenReturn(true);
    assertThrows(RuntimeException.class, () -> authService.register(req));
  }

  @Test
  void initiatePasswordReset_whenEmailExists_shouldSendEmail() throws IOException {
    String email = "alice@example.com";
    User user =
        User.builder()
            .id(1L)
            .username("alice")
            .email(email)
            .password("encoded")
            .roles(Set.of("ROLE_USER"))
            .enabled(true)
            .build();
    when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
    when(passwordTokenRepository.findByUser(user)).thenReturn(Optional.empty());
    when(passwordTokenRepository.save(any(PasswordResetToken.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    authService.initiatePasswordReset(email);

    verify(emailService).sendPasswordResetEmail(eq(email), anyString());
    verify(passwordTokenRepository).save(any(PasswordResetToken.class));
  }

  @Test
  void initiatePasswordReset_whenEmailNotFound_shouldNotSendEmail() throws IOException {
    String email = "unknown@example.com";
    when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

    authService.initiatePasswordReset(email);

    verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
    verify(passwordTokenRepository, never()).save(any());
  }

  @Test
  void resetPassword_withValidToken_shouldUpdatePassword() {
    User user =
        User.builder()
            .id(1L)
            .username("alice")
            .email("alice@example.com")
            .password("oldPassword")
            .roles(Set.of("ROLE_USER"))
            .enabled(true)
            .build();
    PasswordResetToken token = new PasswordResetToken("valid-token", user);

    ResetPasswordRequest request = new ResetPasswordRequest();
    request.setToken("valid-token");
    request.setNewPassword("NewPassword123!");
    request.setConfirmPassword("NewPassword123!");

    when(passwordTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));
    when(passwordEncoder.encode("NewPassword123!")).thenReturn("encoded-new-password");

    authService.resetPassword(request);

    verify(userRepository).save(user);
    assertEquals("encoded-new-password", user.getPassword());
    assertTrue(token.isUsed());
    verify(passwordTokenRepository).save(token);
  }

  @Test
  void resetPassword_withInvalidToken_shouldThrow() {
    ResetPasswordRequest request = new ResetPasswordRequest();
    request.setToken("invalid-token");
    request.setNewPassword("NewPassword123!");
    request.setConfirmPassword("NewPassword123!");

    when(passwordTokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> authService.resetPassword(request));
    assertEquals("Invalid reset token", exception.getMessage());
  }

  @Test
  void resetPassword_withExpiredToken_shouldThrow() {
    User user =
        User.builder()
            .id(1L)
            .username("alice")
            .email("alice@example.com")
            .password("oldPassword")
            .roles(Set.of("ROLE_USER"))
            .enabled(true)
            .build();
    PasswordResetToken token = new PasswordResetToken("expired-token", user);
    token.setExpiryDate(LocalDateTime.now().minusHours(1)); // expired

    ResetPasswordRequest request = new ResetPasswordRequest();
    request.setToken("expired-token");
    request.setNewPassword("NewPassword123!");
    request.setConfirmPassword("NewPassword123!");

    when(passwordTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(token));

    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> authService.resetPassword(request));
    assertEquals("Reset token has expired", exception.getMessage());
  }

  @Test
  void resetPassword_withUsedToken_shouldThrow() {
    User user =
        User.builder()
            .id(1L)
            .username("alice")
            .email("alice@example.com")
            .password("oldPassword")
            .roles(Set.of("ROLE_USER"))
            .enabled(true)
            .build();
    PasswordResetToken token = new PasswordResetToken("used-token", user);
    token.setUsed(true);

    ResetPasswordRequest request = new ResetPasswordRequest();
    request.setToken("used-token");
    request.setNewPassword("NewPassword123!");
    request.setConfirmPassword("NewPassword123!");

    when(passwordTokenRepository.findByToken("used-token")).thenReturn(Optional.of(token));

    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> authService.resetPassword(request));
    assertEquals("Reset token has already been used", exception.getMessage());
  }

  @Test
  void resetPassword_withMismatchedPasswords_shouldThrow() {
    User user =
        User.builder()
            .id(1L)
            .username("alice")
            .email("alice@example.com")
            .password("oldPassword")
            .roles(Set.of("ROLE_USER"))
            .enabled(true)
            .build();
    PasswordResetToken token = new PasswordResetToken("valid-token", user);

    ResetPasswordRequest request = new ResetPasswordRequest();
    request.setToken("valid-token");
    request.setNewPassword("NewPassword123!");
    request.setConfirmPassword("DifferentPassword!");

    when(passwordTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));

    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> authService.resetPassword(request));
    assertEquals("Password policy not met", exception.getMessage());
  }

  @Test
  void resetPassword_withShortPassword_shouldThrow() {
    User user =
        User.builder()
            .id(1L)
            .username("alice")
            .email("alice@example.com")
            .password("oldPassword")
            .roles(Set.of("ROLE_USER"))
            .enabled(true)
            .build();
    PasswordResetToken token = new PasswordResetToken("valid-token", user);

    ResetPasswordRequest request = new ResetPasswordRequest();
    request.setToken("valid-token");
    request.setNewPassword("short");
    request.setConfirmPassword("short");

    when(passwordTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));

    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> authService.resetPassword(request));
    assertEquals("Password policy not met", exception.getMessage());
  }

  @Test
  void register_withUsernameExists_shouldThrow() {
    RegisterRequest req =
        new RegisterRequest("existinguser", "new@example.com", "Password123!", "Password123!");
    when(userRepository.existsByEmail(req.getEmail())).thenReturn(false);
    when(userRepository.existsByUsername(req.getUsername())).thenReturn(true);

    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> authService.register(req));
    assertEquals("Username already exists", exception.getMessage());
  }

  @Test
  void register_withPasswordMismatch_shouldThrow() {
    RegisterRequest req =
        new RegisterRequest("newuser", "new@example.com", "Password123!", "DifferentPassword!");
    when(userRepository.existsByEmail(req.getEmail())).thenReturn(false);
    when(userRepository.existsByUsername(req.getUsername())).thenReturn(false);

    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> authService.register(req));
    assertEquals("password policy not met", exception.getMessage());
  }

  @Test
  void register_withShortPassword_shouldThrow() {
    RegisterRequest req = new RegisterRequest("newuser", "new@example.com", "short", "short");
    when(userRepository.existsByEmail(req.getEmail())).thenReturn(false);
    when(userRepository.existsByUsername(req.getUsername())).thenReturn(false);

    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> authService.register(req));
    assertEquals("password policy not met", exception.getMessage());
  }

  @Test
  void register_withNullPassword_shouldThrow() {
    RegisterRequest req = new RegisterRequest("newuser", "new@example.com", null, null);
    when(userRepository.existsByEmail(req.getEmail())).thenReturn(false);
    when(userRepository.existsByUsername(req.getUsername())).thenReturn(false);

    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> authService.register(req));
    assertEquals("password policy not met", exception.getMessage());
  }

  @Test
  void register_shouldNormalizeEmailAndUsername() {
    RegisterRequest req = new RegisterRequest();
    req.setUsername("  ALICE  ");
    req.setEmail("  ALICE@EXAMPLE.COM  ");
    req.setPassword("Password123!");
    req.setConfirmPassword("Password123!");

    when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
    when(userRepository.existsByUsername("alice")).thenReturn(false);
    when(passwordEncoder.encode(any())).thenReturn("encoded");

    authService.register(req);

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(captor.capture());
    User saved = captor.getValue();
    assertEquals("alice", saved.getUsername());
    assertEquals("alice@example.com", saved.getEmail());
  }

  @Test
  void register_whenDatabaseError_shouldThrowDatabaseException() {
    RegisterRequest req =
        new RegisterRequest("newuser", "new@example.com", "Password123!", "Password123!");
    when(userRepository.existsByEmail(req.getEmail())).thenReturn(false);
    when(userRepository.existsByUsername(req.getUsername())).thenReturn(false);
    when(passwordEncoder.encode(any())).thenReturn("encoded");
    when(userRepository.save(any()))
        .thenThrow(new org.springframework.dao.DataIntegrityViolationException("Duplicate"));

    AuthService.DatabaseException exception =
        assertThrows(AuthService.DatabaseException.class, () -> authService.register(req));
    assertEquals("unable to save user", exception.getMessage());
  }

  @Test
  void findByUsername_whenUserExists_shouldReturnUser() {
    User user =
        User.builder()
            .id(1L)
            .username("alice")
            .email("alice@example.com")
            .password("encoded")
            .roles(Set.of("ROLE_USER"))
            .enabled(true)
            .build();
    when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

    User found = authService.findByUsername("alice");

    assertEquals("alice", found.getUsername());
    assertEquals("alice@example.com", found.getEmail());
  }

  @Test
  void findByUsername_whenUserNotFound_shouldThrow() {
    when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

    assertThrows(
        org.springframework.security.core.userdetails.UsernameNotFoundException.class,
        () -> authService.findByUsername("unknown"));
  }

  @Test
  void findByUsername_shouldNormalizeUsername() {
    User user =
        User.builder()
            .id(1L)
            .username("alice")
            .email("alice@example.com")
            .password("encoded")
            .roles(Set.of("ROLE_USER"))
            .enabled(true)
            .build();
    when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

    User found = authService.findByUsername("  ALICE  ");

    assertEquals("alice", found.getUsername());
  }

  @Test
  void loadUserByUsername_whenUserExists_shouldReturnUserDetails() {
    User user =
        User.builder()
            .id(1L)
            .username("alice")
            .email("alice@example.com")
            .password("encoded")
            .roles(Set.of("ROLE_USER"))
            .enabled(true)
            .build();
    when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

    org.springframework.security.core.userdetails.UserDetails userDetails =
        authService.loadUserByUsername("alice");

    assertEquals("alice", userDetails.getUsername());
    assertEquals("encoded", userDetails.getPassword());
    assertTrue(userDetails.isEnabled());
    assertTrue(
        userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
  }

  @Test
  void loadUserByUsername_whenUserNotFound_shouldThrow() {
    when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

    assertThrows(
        org.springframework.security.core.userdetails.UsernameNotFoundException.class,
        () -> authService.loadUserByUsername("unknown"));
  }

  @Test
  void loadUserByUsername_shouldNormalizeUsername() {
    User user =
        User.builder()
            .id(1L)
            .username("alice")
            .email("alice@example.com")
            .password("encoded")
            .roles(Set.of("ROLE_USER"))
            .enabled(true)
            .build();
    when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

    org.springframework.security.core.userdetails.UserDetails userDetails =
        authService.loadUserByUsername("  ALICE  ");

    assertEquals("alice", userDetails.getUsername());
  }

  @Test
  void createPasswordResetToken_shouldCreateAndSaveToken() {
    User user =
        User.builder()
            .id(1L)
            .username("alice")
            .email("alice@example.com")
            .password("encoded")
            .roles(Set.of("ROLE_USER"))
            .enabled(true)
            .build();

    when(passwordTokenRepository.findByUser(user)).thenReturn(Optional.empty());
    when(passwordTokenRepository.save(any(PasswordResetToken.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    PasswordResetToken result = authService.createPasswordResetToken(user);

    assertNotNull(result);
    assertNotNull(result.getToken());
    assertEquals(user, result.getUser());
    verify(passwordTokenRepository).save(any(PasswordResetToken.class));
  }

  @Test
  void initiatePasswordReset_shouldNormalizeEmail() throws IOException {
    String email = "  ALICE@EXAMPLE.COM  ";
    User user =
        User.builder()
            .id(1L)
            .username("alice")
            .email("alice@example.com")
            .password("encoded")
            .roles(Set.of("ROLE_USER"))
            .enabled(true)
            .build();

    when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
    when(passwordTokenRepository.findByUser(user)).thenReturn(Optional.empty());
    when(passwordTokenRepository.save(any(PasswordResetToken.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    authService.initiatePasswordReset(email);

    verify(userRepository).findByEmail("alice@example.com");
    verify(emailService).sendPasswordResetEmail(eq("alice@example.com"), anyString());
  }

  @Test
  void resetPassword_withNullPassword_shouldThrow() {
    User user =
        User.builder()
            .id(1L)
            .username("alice")
            .email("alice@example.com")
            .password("oldPassword")
            .roles(Set.of("ROLE_USER"))
            .enabled(true)
            .build();
    PasswordResetToken token = new PasswordResetToken("valid-token", user);

    ResetPasswordRequest request = new ResetPasswordRequest();
    request.setToken("valid-token");
    request.setNewPassword(null);
    request.setConfirmPassword(null);

    when(passwordTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));

    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> authService.resetPassword(request));
    assertEquals("Password policy not met", exception.getMessage());
  }
}
