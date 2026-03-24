package com.example.demo.unitTests.Security;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.security.JwtAuthenticationFilter;
import com.example.demo.security.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

  @Mock private JwtUtil jwtUtil;
  @Mock private UserDetailsService userDetailsService;
  @Mock private HttpServletRequest request;
  @Mock private HttpServletResponse response;
  @Mock private FilterChain filterChain;

  private JwtAuthenticationFilter filter;

  @BeforeEach
  void setUp() {
    filter = new JwtAuthenticationFilter(jwtUtil, userDetailsService);
    SecurityContextHolder.clearContext();
  }

  @Test
  void doFilter_withValidToken_shouldSetAuthentication() throws Exception {
    String token = "valid.jwt.token";
    String username = "testuser";
    UserDetails userDetails =
        User.withUsername(username).password("password").authorities("ROLE_USER").build();

    when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
    when(request.getServletPath()).thenReturn("/api/pets/1");
    when(jwtUtil.validateToken(token)).thenReturn(true);
    when(jwtUtil.extractUsername(token)).thenReturn(username);
    when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);

    filter.doFilter(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verify(userDetailsService).loadUserByUsername(username);
    assertNotNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void doFilter_withInvalidToken_shouldNotSetAuthentication() throws Exception {
    String token = "invalid.jwt.token";

    when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
    when(request.getServletPath()).thenReturn("/api/pets/1");
    when(jwtUtil.validateToken(token)).thenReturn(false);

    filter.doFilter(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verify(userDetailsService, never()).loadUserByUsername(anyString());
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void doFilter_withNoAuthorizationHeader_shouldNotSetAuthentication() throws Exception {
    when(request.getHeader("Authorization")).thenReturn(null);
    when(request.getServletPath()).thenReturn("/api/pets/1");

    filter.doFilter(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verify(jwtUtil, never()).validateToken(anyString());
    verify(userDetailsService, never()).loadUserByUsername(anyString());
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void doFilter_withNonBearerToken_shouldNotSetAuthentication() throws Exception {
    when(request.getHeader("Authorization")).thenReturn("Basic sometoken");
    when(request.getServletPath()).thenReturn("/api/pets/1");

    filter.doFilter(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verify(jwtUtil, never()).validateToken(anyString());
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void doFilter_forAuthLoginPath_shouldSkipAuthentication() throws Exception {
    when(request.getServletPath()).thenReturn("/auth/login");

    filter.doFilter(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verify(jwtUtil, never()).validateToken(anyString());
  }

  @Test
  void doFilter_forAuthRegisterPath_shouldSkipAuthentication() throws Exception {
    when(request.getServletPath()).thenReturn("/auth/register");

    filter.doFilter(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verify(jwtUtil, never()).validateToken(anyString());
  }

  @Test
  void doFilter_forAuthResetPasswordPath_shouldSkipAuthentication() throws Exception {
    when(request.getServletPath()).thenReturn("/auth/reset-password");

    filter.doFilter(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verify(jwtUtil, never()).validateToken(anyString());
  }

  @Test
  void doFilter_forAuthProfilePath_shouldRequireAuthentication() throws Exception {
    String token = "valid.jwt.token";
    String username = "testuser";
    UserDetails userDetails =
        User.withUsername(username).password("password").authorities("ROLE_USER").build();

    when(request.getServletPath()).thenReturn("/auth/profile");
    when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
    when(jwtUtil.validateToken(token)).thenReturn(true);
    when(jwtUtil.extractUsername(token)).thenReturn(username);
    when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);

    filter.doFilter(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verify(jwtUtil).validateToken(token);
    verify(userDetailsService).loadUserByUsername(username);
    assertNotNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void doFilter_whenAuthenticationAlreadyExists_shouldNotOverwrite() throws Exception {
    String token = "valid.jwt.token";
    String username = "testuser";

    // Pre-set authentication in the context
    UserDetails existingUser =
        User.withUsername("existinguser").password("password").authorities("ROLE_USER").build();
    var existingAuth =
        new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
            existingUser, null, existingUser.getAuthorities());
    SecurityContextHolder.getContext().setAuthentication(existingAuth);

    when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
    when(request.getServletPath()).thenReturn("/api/pets/1");
    when(jwtUtil.validateToken(token)).thenReturn(true);
    when(jwtUtil.extractUsername(token)).thenReturn(username);

    filter.doFilter(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    // Should not load user because authentication already exists
    verify(userDetailsService, never()).loadUserByUsername(anyString());
  }

  @Test
  void doFilter_withEmptyBearerToken_shouldNotSetAuthentication() throws Exception {
    when(request.getHeader("Authorization")).thenReturn("Bearer ");
    when(request.getServletPath()).thenReturn("/api/pets/1");
    when(jwtUtil.validateToken("")).thenReturn(false);

    filter.doFilter(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void doFilter_whenUsernameIsNull_shouldNotSetAuthentication() throws Exception {
    String token = "valid.jwt.token";

    when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
    when(request.getServletPath()).thenReturn("/api/pets/1");
    when(jwtUtil.validateToken(token)).thenReturn(true);
    when(jwtUtil.extractUsername(token)).thenReturn(null);

    filter.doFilter(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verify(userDetailsService, never()).loadUserByUsername(anyString());
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }
}
