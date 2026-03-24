package com.example.demo.auth;

import com.example.demo.auth.models.PasswordResetToken;
import com.example.demo.auth.models.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordTokenRepository extends JpaRepository<PasswordResetToken, Long> {

  Optional<PasswordResetToken> findByToken(String token);

  Optional<PasswordResetToken> findByUser(User user);
}
