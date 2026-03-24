package com.example.demo.unitTests.Auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.auth.AuthController;
import com.example.demo.auth.AuthService;
import com.example.demo.auth.dto.AuthResponse;
import com.example.demo.auth.dto.LoginRequest;
import com.example.demo.auth.dto.ProfileResponse;
import com.example.demo.auth.dto.RegisterRequest;
import com.example.demo.auth.dto.ResetPasswordRequest;
import com.example.demo.auth.models.User;
import com.example.demo.security.JwtUtil;
import java.io.IOException;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

  @Mock private AuthService authService;
  @Mock private AuthenticationManager authenticationManager;
  @Mock private JwtUtil jwtUtil;

  @InjectMocks private AuthController authController;

  private User testUser;

  @BeforeEach
  void setUp() {
    testUser =
        User.builder()
            .id(1L)
            .username("testuser")
            .email("testuser@example.com")
            .password("encoded")
            .roles(Set.of("ROLE_USER"))
            .enabled(true)
            .build();
  }

  @Test
  void login_withValidCredentials_shouldReturnToken() {
    LoginRequest request = new LoginRequest();
    request.setUsername("testuser");
    request.setPassword("password123");

    UserDetails userDetails =
        org.springframework.security.core.userdetails.User.withUsername("testuser")
            .password("encoded")
            .authorities("ROLE_USER")
            .build();

    Authentication authentication =
        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

    when(authenticationManager.authenticate(any())).thenReturn(authentication);
    when(jwtUtil.generateToken("testuser")).thenReturn("jwt.token.here");

    ResponseEntity<AuthResponse> response = authController.login(request);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("jwt.token.here", response.getBody().token());
    assertEquals("Bearer", response.getBody().type());
    assertEquals("testuser", response.getBody().username());
  }

  @Test
  void login_withInvalidCredentials_shouldThrowException() {
    LoginRequest request = new LoginRequest();
    request.setUsername("testuser");
    request.setPassword("wrongpassword");

    when(authenticationManager.authenticate(any()))
        .thenThrow(new BadCredentialsException("Bad credentials"));

    assertThrows(BadCredentialsException.class, () -> authController.login(request));
  }

  @Test
  void register_withValidRequest_shouldCallAuthService() {
    RegisterRequest request = new RegisterRequest();
    request.setUsername("newuser");
    request.setEmail("newuser@example.com");
    request.setPassword("Password123!");
    request.setConfirmPassword("Password123!");

    when(authService.register(any(RegisterRequest.class))).thenReturn(testUser);

    authController.register(request);

    verify(authService).register(any(RegisterRequest.class));
  }

  @Test
  void register_withExistingEmail_shouldThrowException() {
    RegisterRequest request = new RegisterRequest();
    request.setUsername("newuser");
    request.setEmail("existing@example.com");
    request.setPassword("Password123!");
    request.setConfirmPassword("Password123!");

    when(authService.register(any(RegisterRequest.class)))
        .thenThrow(new IllegalArgumentException("Email is already registered"));

    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> authController.register(request));
    assertEquals("Email is already registered", exception.getMessage());
  }

  @Test
  void register_withExistingUsername_shouldThrowException() {
    RegisterRequest request = new RegisterRequest();
    request.setUsername("existinguser");
    request.setEmail("new@example.com");
    request.setPassword("Password123!");
    request.setConfirmPassword("Password123!");

    when(authService.register(any(RegisterRequest.class)))
        .thenThrow(new IllegalArgumentException("Username already exists"));

    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> authController.register(request));
    assertEquals("Username already exists", exception.getMessage());
  }

  @Test
  void initiatePasswordReset_shouldReturnOk() throws IOException {
    doNothing().when(authService).initiatePasswordReset(anyString());

    ResponseEntity<Void> response = authController.initiatePasswordReset("user@example.com");

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(authService).initiatePasswordReset("user@example.com");
  }

  @Test
  void initiatePasswordReset_withNonExistentEmail_shouldStillReturnOk() throws IOException {
    // This should return OK to prevent email enumeration
    doNothing().when(authService).initiatePasswordReset(anyString());

    ResponseEntity<Void> response = authController.initiatePasswordReset("nonexistent@example.com");

    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void resetPassword_withValidToken_shouldReturnOk() {
    ResetPasswordRequest request = new ResetPasswordRequest();
    request.setToken("valid-token");
    request.setNewPassword("NewPassword123!");
    request.setConfirmPassword("NewPassword123!");

    doNothing().when(authService).resetPassword(any(ResetPasswordRequest.class));

    ResponseEntity<Void> response = authController.resetPassword(request);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(authService).resetPassword(any(ResetPasswordRequest.class));
  }

  @Test
  void resetPassword_withInvalidToken_shouldThrowException() {
    ResetPasswordRequest request = new ResetPasswordRequest();
    request.setToken("invalid-token");
    request.setNewPassword("NewPassword123!");
    request.setConfirmPassword("NewPassword123!");

    doThrow(new IllegalArgumentException("Invalid reset token"))
        .when(authService)
        .resetPassword(any(ResetPasswordRequest.class));

    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> authController.resetPassword(request));
    assertEquals("Invalid reset token", exception.getMessage());
  }

  @Test
  void resetPassword_withExpiredToken_shouldThrowException() {
    ResetPasswordRequest request = new ResetPasswordRequest();
    request.setToken("expired-token");
    request.setNewPassword("NewPassword123!");
    request.setConfirmPassword("NewPassword123!");

    doThrow(new IllegalArgumentException("Reset token has expired"))
        .when(authService)
        .resetPassword(any(ResetPasswordRequest.class));

    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> authController.resetPassword(request));
    assertEquals("Reset token has expired", exception.getMessage());
  }

  @Test
  void getProfile_withAuthenticatedUser_shouldReturnProfile() {
    when(authService.findByUsername("testuser")).thenReturn(testUser);

    UserDetails principal =
        org.springframework.security.core.userdetails.User.withUsername("testuser")
            .password("encoded")
            .authorities("ROLE_USER")
            .build();

    ProfileResponse response = authController.getProfile(principal);

    assertNotNull(response);
    assertEquals(1L, response.id());
    assertEquals("testuser", response.username());
    assertEquals("testuser@example.com", response.email());
    assertEquals(Set.of("ROLE_USER"), response.roles());
  }

  @Test
  void getProfile_whenUserNotFound_shouldThrowException() {
    when(authService.findByUsername("unknownuser"))
        .thenThrow(
            new org.springframework.security.core.userdetails.UsernameNotFoundException(
                "User not found"));

    UserDetails principal =
        org.springframework.security.core.userdetails.User.withUsername("unknownuser")
            .password("encoded")
            .authorities("ROLE_USER")
            .build();

    assertThrows(
        org.springframework.security.core.userdetails.UsernameNotFoundException.class,
        () -> authController.getProfile(principal));
  }
}
