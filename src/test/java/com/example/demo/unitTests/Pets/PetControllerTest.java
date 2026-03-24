package com.example.demo.unitTests.Pets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.auth.AuthService;
import com.example.demo.auth.models.User;
import com.example.demo.pets.PetController;
import com.example.demo.pets.PetService;
import com.example.demo.pets.dto.PetPreviewResponse;
import com.example.demo.pets.dto.PetRequest;
import com.example.demo.pets.dto.PetResponse;
import com.example.demo.pets.models.Species;
import java.security.Principal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class PetControllerTest {

  @Mock private PetService petService;

  @Mock private AuthService authService;

  @Mock private Principal principal;

  @InjectMocks private PetController petController;

  private User testUser;
  private PetResponse testPetResponse;
  private PetRequest testPetRequest;

  @BeforeEach
  void setUp() {
    testUser =
        User.builder()
            .id(1L)
            .username("testuser")
            .email("test@example.com")
            .password("encoded")
            .roles(Set.of("ROLE_USER"))
            .enabled(true)
            .build();

    testPetResponse =
        PetResponse.builder()
            .id(1L)
            .name("Buddy")
            .species(Species.DOG)
            .breed("Golden Retriever")
            .age(3)
            .description("Friendly dog")
            .imageUrl("http://example.com/image.jpg")
            .ownerUsername("testuser")
            .ownerId(1L)
            .build();

    testPetRequest = new PetRequest();
    testPetRequest.setName("Buddy");
    testPetRequest.setSpecies(Species.DOG);
    testPetRequest.setBreed("Golden Retriever");
    testPetRequest.setAge(3);
    testPetRequest.setDescription("Friendly dog");
  }

  @Test
  void getAllPets_shouldReturnAllPetPreviews() {
    PetPreviewResponse previewResponse =
        PetPreviewResponse.builder()
            .id(1L)
            .name("Buddy")
            .species(Species.DOG)
            .imageUrl("http://example.com/image.jpg")
            .build();
    List<PetPreviewResponse> pets = List.of(previewResponse);
    when(petService.getAllPetPreviews()).thenReturn(pets);

    ResponseEntity<List<PetPreviewResponse>> response = petController.getAllPets();

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(1, response.getBody().size());
    assertEquals("Buddy", response.getBody().get(0).getName());
    verify(petService).getAllPetPreviews();
  }

  @Test
  void getPetById_shouldReturnPet() {
    when(petService.getPetById(1L)).thenReturn(testPetResponse);

    ResponseEntity<PetResponse> response = petController.getPetById(1L);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("Buddy", response.getBody().getName());
    verify(petService).getPetById(1L);
  }

  @Test
  void getMyPets_shouldReturnUserPets() {
    List<PetResponse> pets = List.of(testPetResponse);
    when(principal.getName()).thenReturn("testuser");
    when(authService.findByUsername("testuser")).thenReturn(testUser);
    when(petService.getPetsByUser(1L)).thenReturn(pets);

    ResponseEntity<List<PetResponse>> response = petController.getMyPets(principal);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(1, response.getBody().size());
    verify(authService).findByUsername("testuser");
    verify(petService).getPetsByUser(1L);
  }

  @Test
  void createPet_shouldCreateAndReturnPet() {
    when(principal.getName()).thenReturn("testuser");
    when(authService.findByUsername("testuser")).thenReturn(testUser);
    when(petService.createPet(any(PetRequest.class), eq(testUser))).thenReturn(testPetResponse);

    ResponseEntity<PetResponse> response = petController.createPet(testPetRequest, principal);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals("Buddy", response.getBody().getName());
    verify(authService).findByUsername("testuser");
    verify(petService).createPet(any(PetRequest.class), eq(testUser));
  }

  @Test
  void updatePet_shouldUpdateAndReturnPet() {
    PetResponse updatedPet =
        PetResponse.builder()
            .id(1L)
            .name("Buddy Updated")
            .species(Species.DOG)
            .breed("Golden Retriever")
            .age(4)
            .description("Very friendly dog")
            .ownerUsername("testuser")
            .ownerId(1L)
            .build();

    when(principal.getName()).thenReturn("testuser");
    when(authService.findByUsername("testuser")).thenReturn(testUser);
    when(petService.updatePet(eq(1L), any(PetRequest.class), eq(testUser))).thenReturn(updatedPet);

    ResponseEntity<PetResponse> response = petController.updatePet(1L, testPetRequest, principal);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("Buddy Updated", response.getBody().getName());
    verify(authService).findByUsername("testuser");
    verify(petService).updatePet(eq(1L), any(PetRequest.class), eq(testUser));
  }

  @Test
  void deletePet_shouldDeletePetAndReturnNoContent() {
    when(principal.getName()).thenReturn("testuser");
    when(authService.findByUsername("testuser")).thenReturn(testUser);

    ResponseEntity<Void> response = petController.deletePet(1L, principal);

    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    verify(authService).findByUsername("testuser");
    verify(petService).deletePet(1L, testUser);
  }

  @Test
  void uploadPetImage_shouldUploadImageAndReturnUpdatedPet() {
    MockMultipartFile file =
        new MockMultipartFile("file", "test.jpg", "image/jpeg", "test image content".getBytes());
    PetResponse updatedPetWithImage =
        PetResponse.builder()
            .id(1L)
            .name("Buddy")
            .species(Species.DOG)
            .breed("Golden Retriever")
            .age(3)
            .description("Friendly dog")
            .imageUrl("http://cloudinary.com/pets/test.jpg")
            .ownerUsername("testuser")
            .ownerId(1L)
            .build();

    when(principal.getName()).thenReturn("testuser");
    when(authService.findByUsername("testuser")).thenReturn(testUser);
    when(petService.uploadPetImage(eq(1L), any(MultipartFile.class), eq(testUser)))
        .thenReturn(updatedPetWithImage);

    ResponseEntity<PetResponse> response = petController.uploadPetImage(1L, file, principal);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("http://cloudinary.com/pets/test.jpg", response.getBody().getImageUrl());
    verify(authService).findByUsername("testuser");
    verify(petService).uploadPetImage(eq(1L), any(MultipartFile.class), eq(testUser));
  }
}
