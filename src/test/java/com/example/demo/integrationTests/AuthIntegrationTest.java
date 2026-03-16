package com.example.demo.integrationTests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.demo.auth.UserRepository;
import com.example.demo.auth.models.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class AuthIntegrationTest {

  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:15")
          .withDatabaseName("it_db")
          .withUsername("it_user")
          .withPassword("it_pass");

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", postgres::getJdbcUrl);
    r.add("spring.datasource.username", postgres::getUsername);
    r.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired UserRepository userRepository;

  @Test
  void save_and_findUser() {
    User user = new User();
    user.setUsername("tc");
    user.setEmail("tc@example.com");
    user.setPassword("encoded");
    user.setEnabled(true);
    userRepository.save(user);

    assertTrue(userRepository.existsByEmail("tc@example.com"));
  }
}
