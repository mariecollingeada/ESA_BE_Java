package com.example.demo.auth;

import com.example.demo.auth.dto.RegisterRequest;
import com.example.demo.auth.dto.ResetPasswordRequest;
import com.example.demo.auth.models.PasswordResetToken;
import com.example.demo.auth.models.User;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService implements UserDetailsService {

  private final UserRepository userRepository;
  private final PasswordTokenRepository passwordTokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final EmailService emailService;

  @Transactional
  public User register(RegisterRequest request) {
    String email = normalizeEmail(request.getEmail());
    String username = normalizeUsername(request.getUsername());

    if (userRepository.existsByEmail(email)) {
      throw new IllegalArgumentException("Email is already registered");
    }
    if (userRepository.existsByUsername(username)) {
      throw new IllegalArgumentException("Username already exists");
    }
    if (!isPasswordAcceptable(request.getPassword(), request.getConfirmPassword())) {
      throw new IllegalArgumentException("password policy not met");
    }

    User user =
        User.builder()
            .username(username)
            .email(email)
            .password(passwordEncoder.encode(request.getPassword()))
            .roles(Set.of("ROLE_USER"))
            .enabled(true)
            .build();

    try {
      return userRepository.save(user);
    } catch (DataIntegrityViolationException ex) {
      throw new DatabaseException("unable to save user");
    }
  }

  private String normalizeEmail(String email) {
    return email == null ? null : email.trim().toLowerCase();
  }

  private String normalizeUsername(String username) {
    return username == null ? null : username.trim().toLowerCase();
  }

  private boolean isPasswordAcceptable(String password, String confirmPassword) {
    log.info(password);
    log.info(confirmPassword);
    if (password == null || confirmPassword == null) {
      log.info("Password or confirm password is null");
      return false;
    }
    if (!password.equals(confirmPassword)) {
      log.info("Passwords do not match");
      return false;
    }
    return password.length() >= 8;
  }

  public void initiatePasswordReset(String email) throws IOException {
    String normalizedEmail = normalizeEmail(email);
    var userOptional = userRepository.findByEmail(normalizedEmail);
    if (userOptional.isEmpty()) {
      // Silently return to prevent email enumeration
      log.info("Password reset requested for non-existent email: {}", normalizedEmail);
      return;
    }

    User user = userOptional.get();
    PasswordResetToken resetToken = createPasswordResetToken(user);

    emailService.sendPasswordResetEmail(normalizedEmail, resetToken.getToken());
    log.info("Password reset email sent to: {}", normalizedEmail);
  }

  public User findByUsername(String username) {
    String normalizedUsername = normalizeUsername(username);
    return userRepository
        .findByUsername(normalizedUsername)
        .orElseThrow(() -> new UsernameNotFoundException("User not found: " + normalizedUsername));
  }

  @Transactional
  public void resetPassword(ResetPasswordRequest request) {
    PasswordResetToken token = getValidToken(request.getToken());

    if (!isPasswordAcceptable(request.getNewPassword(), request.getConfirmPassword())) {
      throw new IllegalArgumentException("Password policy not met");
    }

    User user = token.getUser();
    user.setPassword(passwordEncoder.encode(request.getNewPassword()));
    userRepository.save(user);

    token.setUsed(true);
    passwordTokenRepository.save(token);

    log.info("Password reset successful for user: {}", user.getUsername());
  }

  private PasswordResetToken getValidToken(String token) {
    PasswordResetToken resetToken =
        passwordTokenRepository
            .findByToken(token)
            .orElseThrow(() -> new IllegalArgumentException("Invalid reset token"));

    if (resetToken.isExpired()) {
      throw new IllegalArgumentException("Reset token has expired");
    }

    if (resetToken.isUsed()) {
      throw new IllegalArgumentException("Reset token has already been used");
    }

    return resetToken;
  }

  public static class DatabaseException extends RuntimeException {
    public DatabaseException(String msg, Throwable cause) {
      super(msg, cause);
    }

    public DatabaseException(String msg) {
      super(msg);
    }
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    String normalizedUsername = normalizeUsername(username);

    com.example.demo.auth.models.User user =
        userRepository
            .findByUsername(normalizedUsername)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

    return new org.springframework.security.core.userdetails.User(
        user.getUsername(),
        user.getPassword(),
        user.isEnabled(), // enabled
        true, // accountNonExpired
        true, // credentialsNonExpired
        true, // accountNonLocked
        user.getRoles().stream().map(SimpleGrantedAuthority::new).toList());
  }

  public PasswordResetToken createPasswordResetToken(User user) {
    String token = UUID.randomUUID().toString();

    return passwordTokenRepository
        .findByUser(user)
        .map(
            existing -> {
              existing.setToken(token);
              existing.setUsed(false);
              existing.setExpiryDate(LocalDateTime.now().plusHours(1));
              return passwordTokenRepository.save(existing);
            })
        .orElseGet(
            () -> {
              PasswordResetToken newToken = new PasswordResetToken();
              newToken.setUser(user);
              newToken.setToken(token);
              newToken.setUsed(false);
              newToken.setExpiryDate(LocalDateTime.now().plusHours(1));
              return passwordTokenRepository.save(newToken);
            });
  }
}
