package com.example.demo.auth;

import java.util.Set;

import com.example.demo.auth.dto.LoginRequest;
import com.example.demo.auth.dto.RegisterRequest;
import com.example.demo.auth.dto.ResetPasswordRequest;
import com.example.demo.auth.models.User;

import lombok.RequiredArgsConstructor;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public String login(LoginRequest request) {
        // TODO: validate credentials against user store and return a JWT / session token
        throw new UnsupportedOperationException("login not yet implemented");
    }

    @Transactional
    public User register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already registered");
        }
        if(userRepository.existsByUsername(request.getUsername())){
            throw new IllegalArgumentException("Username already exists");
        }
        if(!isPasswordAcceptable(request.getPassword(), request.getConfirmPassword())){
            throw new IllegalArgumentException("password policy not met");
        }

        User user = User.builder()
            .username(request.getUsername())
            .email(request.getEmail())
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

    private boolean isPasswordAcceptable(String password, String confirmPassword){
        if (!password.equals(confirmPassword)) {
            return false;
        }
        return password.length() >= 8;

    }

    public void initiatePasswordReset(String email) {
        // TODO: look up user by email and send a password-reset link / token
        throw new UnsupportedOperationException("initiatePasswordReset not yet implemented");
    }

    public void resetPassword(ResetPasswordRequest request) {
        // TODO: validate token, check that passwords match, hash new password, persist
        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }
        throw new UnsupportedOperationException("resetPassword not yet implemented");
    }

    public static class DatabaseException extends RuntimeException {
        public DatabaseException(String msg, Throwable cause) { super(msg, cause); }
        public DatabaseException(String msg) { super(msg); }
    }

    @Override
    public UserDetails loadUserByUsername(String username)
        throws UsernameNotFoundException {

        com.example.demo.auth.models.User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return new org.springframework.security.core.userdetails.User(
            user.getUsername(),
            user.getPassword(),
            user.isEnabled(),   // enabled
            true,               // accountNonExpired
            true,               // credentialsNonExpired
            true,               // accountNonLocked
            user.getRoles().stream()
                .map(SimpleGrantedAuthority::new)
                .toList()
        );
    }
}

