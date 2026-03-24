package com.example.demo.auth;

import com.example.demo.auth.dto.AuthResponse;
import com.example.demo.auth.dto.LoginRequest;
import com.example.demo.auth.dto.ProfileResponse;
import com.example.demo.auth.dto.RegisterRequest;
import com.example.demo.auth.dto.ResetPasswordRequest;
import com.example.demo.security.JwtUtil;
import jakarta.validation.Valid;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;
  private final AuthenticationManager authenticationManager;
  private final JwtUtil jwtUtil;

  /** POST /auth/login Authenticates the user and returns a token. */
  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
    log.info("Login attempt for user: {}", request.getUsername());
    Authentication authentication =
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

    // principal should be your UserDetails implementation (or Spring's User)
    UserDetails userDetails = (UserDetails) authentication.getPrincipal();

    // generate token: adapt this call to your JwtUtil signature if needed
    String token = jwtUtil.generateToken(userDetails.getUsername());

    var response = new AuthResponse(token, "Bearer", userDetails.getUsername());
    return ResponseEntity.ok(response);
  }

  /** POST /auth/register Registers a new user account. */
  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  public void register(@RequestBody @Valid RegisterRequest request) {
    log.info("Register attempt for user: {}", request.getUsername());
    authService.register(request);
  }

  /**
   * POST /auth/reset-password/initiate?email=... Sends a password-reset email to the given address.
   */
  @PostMapping("/reset-password/initiate")
  public ResponseEntity<Void> initiatePasswordReset(@RequestParam String email) throws IOException {
    authService.initiatePasswordReset(email);
    return ResponseEntity.ok().build();
  }

  /**
   * POST /auth/reset-password Completes the password reset using the provided token and new
   * password.
   */
  @PostMapping("/reset-password")
  public ResponseEntity<Void> resetPassword(@RequestBody ResetPasswordRequest request) {
    authService.resetPassword(request);
    return ResponseEntity.ok().build();
  }

  /**
   * GET /auth/profile Returns the profile of the currently authenticated user. Requires a valid JWT
   * in the Authorization header.
   */
  @GetMapping("/profile")
  public ProfileResponse getProfile(@AuthenticationPrincipal UserDetails principal) {
    com.example.demo.auth.models.User user = authService.findByUsername(principal.getUsername());
    return new ProfileResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRoles());
  }
}
