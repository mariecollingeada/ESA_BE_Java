package com.example.demo.unitTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.demo.auth.AuthService;
import com.example.demo.auth.UserRepository;
import com.example.demo.auth.dto.RegisterRequest;
import com.example.demo.auth.models.User;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks
    private AuthService authService;

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
        RegisterRequest req = new RegisterRequest("a","a@a.com","pw","pw");
        when(userRepository.existsByEmail(req.getEmail())).thenReturn(true);
        assertThrows(RuntimeException.class, () -> authService.register(req));
    }
}