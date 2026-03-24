package com.example.demo.integrationTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.demo.auth.AuthService;
import com.example.demo.auth.UserRepository;
import com.example.demo.auth.models.User;
import com.example.demo.pets.FavoriteRepository;
import com.example.demo.pets.PetRepository;
import com.example.demo.pets.PetService;
import com.example.demo.pets.dto.PetPreviewResponse;
import com.example.demo.pets.dto.PetRequest;
import com.example.demo.pets.dto.PetResponse;
import com.example.demo.pets.models.Pet;
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
class PetIntegrationTest {

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
  @Autowired private AuthService authService;
  @Autowired private PasswordEncoder passwordEncoder;

  private User testUser;
  private User otherUser;

  @BeforeEach
  void setUp() {
    favoriteRepository.deleteAll();
    petRepository.deleteAll();
    userRepository.deleteAll();

    testUser =
        User.builder()
            .username("petowner")
            .email("petowner@example.com")
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
  }

  @Test
  void createPet_shouldPersistPetInDatabase() {
    PetRequest request = new PetRequest();
    request.setName("Buddy");
    request.setSpecies(Species.DOG);
    request.setBreed("Golden Retriever");
    request.setAge(3);
    request.setDescription("A friendly dog");

    PetResponse response = petService.createPet(request, testUser);

    assertNotNull(response.getId());
    assertEquals("Buddy", response.getName());
    assertEquals(Species.DOG, response.getSpecies());
    assertEquals("Golden Retriever", response.getBreed());
    assertEquals(3, response.getAge());
    assertEquals("A friendly dog", response.getDescription());
    assertEquals(testUser.getUsername(), response.getOwnerUsername());
    assertEquals(testUser.getId(), response.getOwnerId());

    // Verify persisted in database
    Pet savedPet = petRepository.findById(response.getId()).orElseThrow();
    assertEquals("Buddy", savedPet.getName());
    assertEquals(testUser.getId(), savedPet.getUser().getId());
  }

  @Test
  void getPetById_shouldReturnPet() {
    Pet pet =
        Pet.builder()
            .name("Whiskers")
            .species(Species.CAT)
            .breed("Persian")
            .age(2)
            .description("Fluffy cat")
            .user(testUser)
            .build();
    pet = petRepository.save(pet);

    PetResponse response = petService.getPetById(pet.getId());

    assertEquals("Whiskers", response.getName());
    assertEquals(Species.CAT, response.getSpecies());
    assertEquals("Persian", response.getBreed());
    assertEquals(2, response.getAge());
  }

  @Test
  void getPetById_withNonExistentId_shouldThrow() {
    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> petService.getPetById(999L));
    assertEquals("Pet not found", exception.getMessage());
  }

  @Test
  void getAllPetPreviews_shouldReturnAllPets() {
    Pet pet1 = Pet.builder().name("Pet1").species(Species.DOG).user(testUser).build();
    Pet pet2 = Pet.builder().name("Pet2").species(Species.CAT).user(otherUser).build();
    petRepository.save(pet1);
    petRepository.save(pet2);

    List<PetPreviewResponse> previews = petService.getAllPetPreviews();

    assertEquals(2, previews.size());
    assertTrue(previews.stream().anyMatch(p -> p.getName().equals("Pet1")));
    assertTrue(previews.stream().anyMatch(p -> p.getName().equals("Pet2")));
  }

  @Test
  void getPetsByUser_shouldReturnOnlyUserPets() {
    Pet userPet = Pet.builder().name("MyPet").species(Species.DOG).user(testUser).build();
    Pet otherPet = Pet.builder().name("OtherPet").species(Species.CAT).user(otherUser).build();
    petRepository.save(userPet);
    petRepository.save(otherPet);

    List<PetResponse> userPets = petService.getPetsByUser(testUser.getId());

    assertEquals(1, userPets.size());
    assertEquals("MyPet", userPets.getFirst().getName());
  }

  @Test
  void updatePet_byOwner_shouldSucceed() {
    Pet pet =
        Pet.builder()
            .name("OldName")
            .species(Species.DOG)
            .breed("Labrador")
            .age(1)
            .description("Old description")
            .user(testUser)
            .build();
    pet = petRepository.save(pet);

    PetRequest updateRequest = new PetRequest();
    updateRequest.setName("NewName");
    updateRequest.setSpecies(Species.DOG);
    updateRequest.setBreed("Golden Retriever");
    updateRequest.setAge(2);
    updateRequest.setDescription("New description");

    PetResponse response = petService.updatePet(pet.getId(), updateRequest, testUser);

    assertEquals("NewName", response.getName());
    assertEquals("Golden Retriever", response.getBreed());
    assertEquals(2, response.getAge());
    assertEquals("New description", response.getDescription());

    // Verify persisted
    Pet updatedPet = petRepository.findById(pet.getId()).orElseThrow();
    assertEquals("NewName", updatedPet.getName());
  }

  @Test
  void updatePet_byNonOwner_shouldThrow() {
    Pet pet = Pet.builder().name("Pet").species(Species.DOG).user(testUser).build();
    Pet savedPet = petRepository.save(pet);
    final Long petId = savedPet.getId();

    PetRequest updateRequest = new PetRequest();
    updateRequest.setName("Hacked");
    updateRequest.setSpecies(Species.DOG);

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> petService.updatePet(petId, updateRequest, otherUser));
    assertEquals("You can only update your own pets", exception.getMessage());

    // Verify not changed
    Pet unchangedPet = petRepository.findById(petId).orElseThrow();
    assertEquals("Pet", unchangedPet.getName());
  }

  @Test
  void updatePet_nonExistentPet_shouldThrow() {
    PetRequest updateRequest = new PetRequest();
    updateRequest.setName("Name");
    updateRequest.setSpecies(Species.DOG);

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> petService.updatePet(999L, updateRequest, testUser));
    assertEquals("Pet not found", exception.getMessage());
  }

  @Test
  void deletePet_byOwner_shouldSucceed() {
    Pet pet = Pet.builder().name("ToDelete").species(Species.DOG).user(testUser).build();
    pet = petRepository.save(pet);
    Long petId = pet.getId();

    petService.deletePet(petId, testUser);

    assertTrue(petRepository.findById(petId).isEmpty());
  }

  @Test
  void deletePet_byNonOwner_shouldThrow() {
    Pet pet = Pet.builder().name("Protected").species(Species.DOG).user(testUser).build();
    Pet savedPet = petRepository.save(pet);
    final Long petId = savedPet.getId();

    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> petService.deletePet(petId, otherUser));
    assertEquals("You can only delete your own pets", exception.getMessage());

    // Verify still exists
    assertTrue(petRepository.findById(petId).isPresent());
  }

  @Test
  void deletePet_nonExistentPet_shouldThrow() {
    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> petService.deletePet(999L, testUser));
    assertEquals("Pet not found", exception.getMessage());
  }

  @Test
  void createMultiplePets_forSameUser_shouldAllBePersisted() {
    for (int i = 1; i <= 3; i++) {
      PetRequest request = new PetRequest();
      request.setName("Pet" + i);
      request.setSpecies(Species.DOG);
      petService.createPet(request, testUser);
    }

    List<PetResponse> userPets = petService.getPetsByUser(testUser.getId());

    assertEquals(3, userPets.size());
  }

  @Test
  void getAllPets_shouldReturnInDescendingOrderByCreatedAt() throws InterruptedException {
    Pet pet1 = Pet.builder().name("First").species(Species.DOG).user(testUser).build();
    petRepository.save(pet1);

    Thread.sleep(10); // Small delay to ensure different timestamps

    Pet pet2 = Pet.builder().name("Second").species(Species.CAT).user(testUser).build();
    petRepository.save(pet2);

    List<PetPreviewResponse> previews = petService.getAllPetPreviews();

    assertEquals(2, previews.size());
    // Most recent should be first
    assertEquals("Second", previews.getFirst().getName());
    assertEquals("First", previews.get(1).getName());
  }

  @Test
  void createPet_withMinimalData_shouldSucceed() {
    PetRequest request = new PetRequest();
    request.setName("MinimalPet");
    request.setSpecies(Species.OTHER);
    // No breed, age, or description

    PetResponse response = petService.createPet(request, testUser);

    assertNotNull(response.getId());
    assertEquals("MinimalPet", response.getName());
    assertEquals(Species.OTHER, response.getSpecies());
  }

  @Test
  void userDeletion_shouldNotAffectOtherUsersPets() {
    // Create pets for both users
    Pet testUserPet = Pet.builder().name("TestPet").species(Species.DOG).user(testUser).build();
    Pet otherUserPet = Pet.builder().name("OtherPet").species(Species.CAT).user(otherUser).build();
    petRepository.save(testUserPet);
    petRepository.save(otherUserPet);

    // Delete test user's pet
    petService.deletePet(testUserPet.getId(), testUser);

    // Other user's pet should still exist
    assertTrue(petRepository.findById(otherUserPet.getId()).isPresent());
    assertEquals(1, petRepository.count());
  }
}
