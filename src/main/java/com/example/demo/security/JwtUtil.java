package com.example.demo.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

  private final SecretKey key;
  private final long validityMs;

  public JwtUtil(
      @Value("${app.jwt.secret:change_this_dev_secret_replace_me_32bytes}") String secret,
      @Value("${app.jwt.ttl-ms:86400000}") long ttlMs) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.validityMs = ttlMs;
  }

  public String generateToken(String username) {
    Date now = new Date();
    Date expiry = new Date(now.getTime() + validityMs);
    return Jwts.builder()
        .subject(username)
        .issuedAt(now)
        .expiration(expiry)
        .signWith(key)
        .compact();
  }

  public String extractUsername(String token) {
    return parseClaims(token).getSubject();
  }

  public boolean validateToken(String token) {
    try {
      parseClaims(token);
      return true;
    } catch (JwtException | IllegalArgumentException ex) {
      return false;
    }
  }

  private Claims parseClaims(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
  }
}
