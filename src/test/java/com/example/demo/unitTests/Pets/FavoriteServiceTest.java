package com.example.demo.unitTests.Pets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.auth.models.User;
import com.example.demo.pets.FavoriteRepository;
import com.example.demo.pets.FavoriteService;
import com.example.demo.pets.PetRepository;
import com.example.demo.pets.dto.PetPreviewResponse;
import com.example.demo.pets.models.Favorite;
import com.example.demo.pets.models.Pet;
import com.example.demo.pets.models.Species;
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

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

  @Mock private FavoriteRepository favoriteRepository;
  @Mock private PetRepository petRepository;

  @InjectMocks private FavoriteService favoriteService;

  private User testUser;
  private Pet testPet;
  private Favorite testFavorite;

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

    testPet =
        Pet.builder()
            .id(1L)
            .name("Buddy")
            .species(Species.DOG)
            .breed("Golden Retriever")
            .age(3)
            .description("Friendly dog")
            .imageUrl("http://example.com/image.jpg")
            .user(testUser)
            .build();

    testFavorite = Favorite.builder().id(1L).user(testUser).pet(testPet).build();
  }

  // ==================== addFavorite ====================

  @Test
  void addFavorite_shouldSaveFavorite() {
    when(petRepository.findById(1L)).thenReturn(Optional.of(testPet));
    when(favoriteRepository.existsByUserAndPet(testUser, testPet)).thenReturn(false);

    favoriteService.addFavorite(1L, testUser);

    ArgumentCaptor<Favorite> captor = ArgumentCaptor.forClass(Favorite.class);
    verify(favoriteRepository).save(captor.capture());
    Favorite saved = captor.getValue();
    assertEquals(testUser, saved.getUser());
    assertEquals(testPet, saved.getPet());
  }

  @Test
  void addFavorite_whenPetNotFound_shouldThrow() {
    when(petRepository.findById(99L)).thenReturn(Optional.empty());

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> favoriteService.addFavorite(99L, testUser));

    assertEquals("Pet not found", exception.getMessage());
    verify(favoriteRepository, never()).save(any());
  }

  @Test
  void addFavorite_whenAlreadyFavorited_shouldThrow() {
    when(petRepository.findById(1L)).thenReturn(Optional.of(testPet));
    when(favoriteRepository.existsByUserAndPet(testUser, testPet)).thenReturn(true);

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> favoriteService.addFavorite(1L, testUser));

    assertEquals("Pet is already in favourites", exception.getMessage());
    verify(favoriteRepository, never()).save(any());
  }

  // ==================== removeFavorite ====================

  @Test
  void removeFavorite_shouldDeleteFavorite() {
    when(petRepository.findById(1L)).thenReturn(Optional.of(testPet));
    when(favoriteRepository.findByUserAndPet(testUser, testPet))
        .thenReturn(Optional.of(testFavorite));

    favoriteService.removeFavorite(1L, testUser);

    verify(favoriteRepository).delete(testFavorite);
  }

  @Test
  void removeFavorite_whenPetNotFound_shouldThrow() {
    when(petRepository.findById(99L)).thenReturn(Optional.empty());

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> favoriteService.removeFavorite(99L, testUser));

    assertEquals("Pet not found", exception.getMessage());
    verify(favoriteRepository, never()).delete(any());
  }

  @Test
  void removeFavorite_whenNotFavorited_shouldThrow() {
    when(petRepository.findById(1L)).thenReturn(Optional.of(testPet));
    when(favoriteRepository.findByUserAndPet(testUser, testPet)).thenReturn(Optional.empty());

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> favoriteService.removeFavorite(1L, testUser));

    assertEquals("Pet is not in favourites", exception.getMessage());
    verify(favoriteRepository, never()).delete(any());
  }

  // ==================== getUserFavorites ====================

  @Test
  void getUserFavorites_shouldReturnFavoritePets() {
    List<Favorite> favorites = List.of(testFavorite);
    when(favoriteRepository.findByUserOrderByCreatedAtDesc(testUser)).thenReturn(favorites);

    List<PetPreviewResponse> result = favoriteService.getUserFavorites(testUser);

    assertEquals(1, result.size());
    assertEquals("Buddy", result.getFirst().getName());
    assertEquals(Species.DOG, result.getFirst().getSpecies());
    assertTrue(result.getFirst().getIsFavorited());
  }

  @Test
  void getUserFavorites_whenNoFavorites_shouldReturnEmptyList() {
    when(favoriteRepository.findByUserOrderByCreatedAtDesc(testUser)).thenReturn(List.of());

    List<PetPreviewResponse> result = favoriteService.getUserFavorites(testUser);

    assertTrue(result.isEmpty());
  }

  // ==================== isFavorite ====================

  @Test
  void isFavorite_whenFavorited_shouldReturnTrue() {
    when(petRepository.findById(1L)).thenReturn(Optional.of(testPet));
    when(favoriteRepository.existsByUserAndPet(testUser, testPet)).thenReturn(true);

    boolean result = favoriteService.isFavorite(1L, testUser);

    assertTrue(result);
  }

  @Test
  void isFavorite_whenNotFavorited_shouldReturnFalse() {
    when(petRepository.findById(1L)).thenReturn(Optional.of(testPet));
    when(favoriteRepository.existsByUserAndPet(testUser, testPet)).thenReturn(false);

    boolean result = favoriteService.isFavorite(1L, testUser);

    assertFalse(result);
  }

  @Test
  void isFavorite_whenPetNotFound_shouldReturnFalse() {
    when(petRepository.findById(99L)).thenReturn(Optional.empty());

    boolean result = favoriteService.isFavorite(99L, testUser);

    assertFalse(result);
  }

  // ==================== getUserFavoritePetIds ====================

  @Test
  void getUserFavoritePetIds_shouldReturnSetOfPetIds() {
    Set<Long> petIds = Set.of(1L, 2L, 3L);
    when(favoriteRepository.findPetIdsByUser(testUser)).thenReturn(petIds);

    Set<Long> result = favoriteService.getUserFavoritePetIds(testUser);

    assertEquals(3, result.size());
    assertTrue(result.contains(1L));
    assertTrue(result.contains(2L));
    assertTrue(result.contains(3L));
  }

  @Test
  void getUserFavoritePetIds_whenNoFavorites_shouldReturnEmptySet() {
    when(favoriteRepository.findPetIdsByUser(testUser)).thenReturn(Set.of());

    Set<Long> result = favoriteService.getUserFavoritePetIds(testUser);

    assertTrue(result.isEmpty());
  }
}
