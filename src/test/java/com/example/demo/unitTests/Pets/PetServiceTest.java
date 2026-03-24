package com.example.demo.unitTests.Pets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.auth.models.User;
import com.example.demo.pets.CloudinaryService;
import com.example.demo.pets.PetRepository;
import com.example.demo.pets.PetService;
import com.example.demo.pets.dto.PetPreviewResponse;
import com.example.demo.pets.dto.PetRequest;
import com.example.demo.pets.dto.PetResponse;
import com.example.demo.pets.models.Pet;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class PetServiceTest {

  @Mock private PetRepository petRepository;

  @Mock private CloudinaryService cloudinaryService;

  @InjectMocks private PetService petService;

  private User testUser;
  private User otherUser;
  private Pet testPet;
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

    otherUser =
        User.builder()
            .id(2L)
            .username("otheruser")
            .email("other@example.com")
            .password("encoded")
            .roles(Set.of("ROLE_USER"))
            .enabled(true)
            .build();

    testPet =
        Pet.builder()
            .id(1L)
            .name("Buddy")
            .species("Dog")
            .breed("Golden Retriever")
            .age(3)
            .description("Friendly dog")
            .imageUrl("http://example.com/image.jpg")
            .user(testUser)
            .build();

    testPetRequest = new PetRequest();
    testPetRequest.setName("Buddy");
    testPetRequest.setSpecies("Dog");
    testPetRequest.setBreed("Golden Retriever");
    testPetRequest.setAge(3);
    testPetRequest.setDescription("Friendly dog");
  }

  // ==================== getAllPetPreviews ====================

  @Test
  void getAllPetPreviews_shouldReturnListOfPreviews() {
    List<Pet> pets = List.of(testPet);
    when(petRepository.findAllByOrderByCreatedAtDesc()).thenReturn(pets);

    List<PetPreviewResponse> result = petService.getAllPetPreviews();

    assertEquals(1, result.size());
    assertEquals("Buddy", result.get(0).getName());
    assertEquals("Dog", result.get(0).getSpecies());
    assertEquals("http://example.com/image.jpg", result.get(0).getImageUrl());
    verify(petRepository).findAllByOrderByCreatedAtDesc();
  }

  @Test
  void getAllPetPreviews_shouldReturnEmptyListWhenNoPets() {
    when(petRepository.findAllByOrderByCreatedAtDesc()).thenReturn(Collections.emptyList());

    List<PetPreviewResponse> result = petService.getAllPetPreviews();

    assertEquals(0, result.size());
    verify(petRepository).findAllByOrderByCreatedAtDesc();
  }

  // ==================== getAllPets ====================

  @Test
  void getAllPets_shouldReturnListOfPetResponses() {
    List<Pet> pets = List.of(testPet);
    when(petRepository.findAllByOrderByCreatedAtDesc()).thenReturn(pets);

    List<PetResponse> result = petService.getAllPets();

    assertEquals(1, result.size());
    assertEquals("Buddy", result.get(0).getName());
    verify(petRepository).findAllByOrderByCreatedAtDesc();
  }

  // ==================== getPetsByUser ====================

  @Test
  void getPetsByUser_shouldReturnUserPets() {
    List<Pet> pets = List.of(testPet);
    when(petRepository.findByUserId(1L)).thenReturn(pets);

    List<PetResponse> result = petService.getPetsByUser(1L);

    assertEquals(1, result.size());
    assertEquals("Buddy", result.get(0).getName());
    verify(petRepository).findByUserId(1L);
  }

  @Test
  void getPetsByUser_shouldReturnEmptyListWhenUserHasNoPets() {
    when(petRepository.findByUserId(1L)).thenReturn(Collections.emptyList());

    List<PetResponse> result = petService.getPetsByUser(1L);

    assertEquals(0, result.size());
    verify(petRepository).findByUserId(1L);
  }

  // ==================== getPetById ====================

  @Test
  void getPetById_shouldReturnPetWhenFound() {
    when(petRepository.findById(1L)).thenReturn(Optional.of(testPet));

    PetResponse result = petService.getPetById(1L);

    assertNotNull(result);
    assertEquals("Buddy", result.getName());
    assertEquals("Dog", result.getSpecies());
    verify(petRepository).findById(1L);
  }

  @Test
  void getPetById_shouldThrowWhenPetNotFound() {
    when(petRepository.findById(99L)).thenReturn(Optional.empty());

    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> petService.getPetById(99L));

    assertEquals("Pet not found", exception.getMessage());
    verify(petRepository).findById(99L);
  }

  // ==================== createPet ====================

  @Test
  void createPet_shouldSaveAndReturnPet() {
    when(petRepository.save(any(Pet.class))).thenReturn(testPet);

    PetResponse result = petService.createPet(testPetRequest, testUser);

    assertNotNull(result);
    assertEquals("Buddy", result.getName());
    assertEquals("Dog", result.getSpecies());

    ArgumentCaptor<Pet> petCaptor = ArgumentCaptor.forClass(Pet.class);
    verify(petRepository).save(petCaptor.capture());
    Pet savedPet = petCaptor.getValue();
    assertEquals("Buddy", savedPet.getName());
    assertEquals(testUser, savedPet.getUser());
  }

  // ==================== updatePet ====================

  @Test
  void updatePet_shouldUpdateAndReturnPet() {
    when(petRepository.findById(1L)).thenReturn(Optional.of(testPet));
    when(petRepository.save(any(Pet.class))).thenReturn(testPet);

    PetRequest updateRequest = new PetRequest();
    updateRequest.setName("Buddy Updated");
    updateRequest.setSpecies("Dog");
    updateRequest.setBreed("Labrador");
    updateRequest.setAge(4);
    updateRequest.setDescription("Very friendly dog");

    PetResponse result = petService.updatePet(1L, updateRequest, testUser);

    assertNotNull(result);
    verify(petRepository).findById(1L);
    verify(petRepository).save(any(Pet.class));
  }

  @Test
  void updatePet_shouldThrowWhenPetNotFound() {
    when(petRepository.findById(99L)).thenReturn(Optional.empty());

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> petService.updatePet(99L, testPetRequest, testUser));

    assertEquals("Pet not found", exception.getMessage());
    verify(petRepository).findById(99L);
    verify(petRepository, never()).save(any());
  }

  @Test
  void updatePet_shouldThrowWhenUserNotOwner() {
    when(petRepository.findById(1L)).thenReturn(Optional.of(testPet));

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> petService.updatePet(1L, testPetRequest, otherUser));

    assertEquals("You can only update your own pets", exception.getMessage());
    verify(petRepository).findById(1L);
    verify(petRepository, never()).save(any());
  }

  // ==================== deletePet ====================

  @Test
  void deletePet_shouldDeletePet() {
    when(petRepository.findById(1L)).thenReturn(Optional.of(testPet));

    petService.deletePet(1L, testUser);

    verify(petRepository).findById(1L);
    verify(petRepository).delete(testPet);
  }

  @Test
  void deletePet_shouldThrowWhenPetNotFound() {
    when(petRepository.findById(99L)).thenReturn(Optional.empty());

    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> petService.deletePet(99L, testUser));

    assertEquals("Pet not found", exception.getMessage());
    verify(petRepository).findById(99L);
    verify(petRepository, never()).delete(any());
  }

  @Test
  void deletePet_shouldThrowWhenUserNotOwner() {
    when(petRepository.findById(1L)).thenReturn(Optional.of(testPet));

    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> petService.deletePet(1L, otherUser));

    assertEquals("You can only delete your own pets", exception.getMessage());
    verify(petRepository).findById(1L);
    verify(petRepository, never()).delete(any());
  }

  // ==================== uploadPetImage ====================

  @Test
  void uploadPetImage_shouldUploadAndReturnPet() throws IOException {
    MockMultipartFile file =
        new MockMultipartFile("file", "test.jpg", "image/jpeg", "test image content".getBytes());
    when(petRepository.findById(1L)).thenReturn(Optional.of(testPet));
    when(cloudinaryService.uploadImage(file)).thenReturn("http://cloudinary.com/new-image.jpg");
    when(petRepository.save(any(Pet.class))).thenReturn(testPet);

    PetResponse result = petService.uploadPetImage(1L, file, testUser);

    assertNotNull(result);
    verify(petRepository).findById(1L);
    verify(cloudinaryService).uploadImage(file);
    verify(petRepository).save(any(Pet.class));
  }

  @Test
  void uploadPetImage_shouldThrowWhenPetNotFound() {
    MockMultipartFile file =
        new MockMultipartFile("file", "test.jpg", "image/jpeg", "test image content".getBytes());
    when(petRepository.findById(99L)).thenReturn(Optional.empty());

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> petService.uploadPetImage(99L, file, testUser));

    assertEquals("Pet not found", exception.getMessage());
    verify(petRepository).findById(99L);
  }

  @Test
  void uploadPetImage_shouldThrowWhenUserNotOwner() {
    MockMultipartFile file =
        new MockMultipartFile("file", "test.jpg", "image/jpeg", "test image content".getBytes());
    when(petRepository.findById(1L)).thenReturn(Optional.of(testPet));

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> petService.uploadPetImage(1L, file, otherUser));

    assertEquals("You can only update your own pets", exception.getMessage());
    verify(petRepository).findById(1L);
  }

  @Test
  void uploadPetImage_shouldThrowRuntimeExceptionWhenCloudinaryFails() throws IOException {
    MockMultipartFile file =
        new MockMultipartFile("file", "test.jpg", "image/jpeg", "test image content".getBytes());
    when(petRepository.findById(1L)).thenReturn(Optional.of(testPet));
    when(cloudinaryService.uploadImage(file)).thenThrow(new IOException("Upload failed"));

    RuntimeException exception =
        assertThrows(RuntimeException.class, () -> petService.uploadPetImage(1L, file, testUser));

    assertEquals("Failed to upload image", exception.getMessage());
    verify(petRepository).findById(1L);
    verify(cloudinaryService).uploadImage(file);
    verify(petRepository, never()).save(any());
  }
}
