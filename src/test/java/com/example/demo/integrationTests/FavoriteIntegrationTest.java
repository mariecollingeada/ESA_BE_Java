package com.example.demo.integrationTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.demo.auth.UserRepository;
import com.example.demo.auth.models.User;
import com.example.demo.pets.FavoriteRepository;
import com.example.demo.pets.FavoriteService;
import com.example.demo.pets.PetRepository;
import com.example.demo.pets.PetService;
import com.example.demo.pets.dto.PetPreviewResponse;
import com.example.demo.pets.dto.PetRequest;
import com.example.demo.pets.models.Species;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class FavoriteIntegrationTest {

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

  @Autowired private UserRepository userRepository;
  @Autowired private PetRepository petRepository;
  @Autowired private FavoriteRepository favoriteRepository;
  @Autowired private PetService petService;
  @Autowired private FavoriteService favoriteService;
  @Autowired private PasswordEncoder passwordEncoder;

  private User testUser;
  private User otherUser;
  private Long petId;

  @BeforeEach
  void setUp() {
    favoriteRepository.deleteAll();
    petRepository.deleteAll();
    userRepository.deleteAll();

    testUser =
        User.builder()
            .username("favuser")
            .email("favuser@example.com")
            .password(passwordEncoder.encode("Password123!"))
            .roles(Set.of("ROLE_USER"))
            .enabled(true)
            .build();
    testUser = userRepository.save(testUser);

    otherUser =
        User.builder()
            .username("otheruser")
            .email("other@example.com")
            .password(passwordEncoder.encode("Password123!"))
            .roles(Set.of("ROLE_USER"))
            .enabled(true)
            .build();
    otherUser = userRepository.save(otherUser);

    // Create a pet
    PetRequest petRequest = new PetRequest();
    petRequest.setName("TestPet");
    petRequest.setSpecies(Species.DOG);
    petRequest.setBreed("Labrador");
    petId = petService.createPet(petRequest, otherUser).getId();
  }

  @Test
  void addFavorite_shouldPersistFavorite() {
    favoriteService.addFavorite(petId, testUser);

    assertTrue(favoriteService.isFavorite(petId, testUser));
  }

  @Test
  void addFavorite_whenAlreadyFavorited_shouldThrow() {
    favoriteService.addFavorite(petId, testUser);

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> favoriteService.addFavorite(petId, testUser));

    assertEquals("Pet is already in favourites", exception.getMessage());
  }

  @Test
  void removeFavorite_shouldDeleteFavorite() {
    favoriteService.addFavorite(petId, testUser);
    assertTrue(favoriteService.isFavorite(petId, testUser));

    favoriteService.removeFavorite(petId, testUser);

    assertFalse(favoriteService.isFavorite(petId, testUser));
  }

  @Test
  void removeFavorite_whenNotFavorited_shouldThrow() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> favoriteService.removeFavorite(petId, testUser));

    assertEquals("Pet is not in favourites", exception.getMessage());
  }

  @Test
  void getUserFavorites_shouldReturnOnlyUserFavorites() {
    // Create another pet
    PetRequest petRequest = new PetRequest();
    petRequest.setName("AnotherPet");
    petRequest.setSpecies(Species.CAT);
    Long anotherPetId = petService.createPet(petRequest, otherUser).getId();

    // Add favorites for testUser
    favoriteService.addFavorite(petId, testUser);
    favoriteService.addFavorite(anotherPetId, testUser);

    // Add favorite for otherUser (should not appear in testUser's list)
    favoriteService.addFavorite(petId, otherUser);

    List<PetPreviewResponse> favorites = favoriteService.getUserFavorites(testUser);

    assertEquals(2, favorites.size());
    assertTrue(favorites.stream().allMatch(PetPreviewResponse::getIsFavorited));
  }

  @Test
  void getUserFavorites_shouldReturnEmptyListWhenNoFavorites() {
    List<PetPreviewResponse> favorites = favoriteService.getUserFavorites(testUser);

    assertTrue(favorites.isEmpty());
  }

  @Test
  void getAllPetPreviews_shouldIncludeIsFavoritedForAuthenticatedUser() {
    favoriteService.addFavorite(petId, testUser);

    List<PetPreviewResponse> previews = petService.getAllPetPreviews(testUser);

    assertEquals(1, previews.size());
    assertTrue(previews.getFirst().getIsFavorited());
  }

  @Test
  void getAllPetPreviews_shouldReturnFalseIsFavoritedForAnonymousUser() {
    List<PetPreviewResponse> previews = petService.getAllPetPreviews(null);

    assertEquals(1, previews.size());
    assertFalse(previews.getFirst().getIsFavorited());
  }

  @Test
  void getPetById_shouldIncludeIsFavoritedForAuthenticatedUser() {
    favoriteService.addFavorite(petId, testUser);

    var response = petService.getPetById(petId, testUser);

    assertTrue(response.getIsFavorited());
  }

  @Test
  void getPetById_shouldReturnFalseIsFavoritedWhenNotFavorited() {
    var response = petService.getPetById(petId, testUser);

    assertFalse(response.getIsFavorited());
  }
}
