package com.example.demo.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtUtil jwtUtil;
  private final UserDetailsService userDetailsService;

  private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
  private static final String[] PUBLIC_PATHS = {"/auth/**"};

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getServletPath();
    // /auth/profile requires authentication, so don't skip it
    if ("/auth/profile".equals(path)) {
      return false;
    }
    for (String pattern : PUBLIC_PATHS) {
      if (PATH_MATCHER.match(pattern, path)) {
        return true;
      }
    }
    return false;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {

    String header = request.getHeader("Authorization");
    if (header != null && header.startsWith("Bearer ")) {
      String token = header.substring(7);
      if (jwtUtil.validateToken(token)) {
        String username = jwtUtil.extractUsername(token);
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
          UserDetails ud = userDetailsService.loadUserByUsername(username);
          var auth = new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities());
          auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
          SecurityContextHolder.getContext().setAuthentication(auth);
        }
      }
    }
    chain.doFilter(request, response);
  }
}
