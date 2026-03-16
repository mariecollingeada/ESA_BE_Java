package com.example.demo.auth;

import com.example.demo.auth.dto.AuthResponse;
import com.example.demo.auth.dto.LoginRequest;
import com.example.demo.auth.dto.RegisterRequest;
import com.example.demo.auth.dto.ResetPasswordRequest;
import com.example.demo.security.JwtUtil;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    /**
     * POST /auth/login
     * Authenticates the user and returns a token.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        // principal should be your UserDetails implementation (or Spring's User)
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // generate token: adapt this call to your JwtUtil signature if needed
        String token = jwtUtil.generateToken(userDetails.getUsername());

        var response = new AuthResponse(token, "Bearer", userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    /**
     * POST /auth/register
     * Registers a new user account.
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(201).body(Map.of("status","created"));
    }

    /**
     * POST /auth/reset-password/initiate?email=...
     * Sends a password-reset email to the given address.
     */
    @PostMapping("/reset-password/initiate")
    public ResponseEntity<Void> initiatePasswordReset(@RequestParam String email) {
        authService.initiatePasswordReset(email);
        return ResponseEntity.ok().build();
    }

    /**
     * POST /auth/reset-password
     * Completes the password reset using the provided token and new password.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok().build();
    }
}

