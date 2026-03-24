package com.example.demo.pets;

import com.example.demo.auth.models.User;
import com.example.demo.pets.dto.PetPreviewResponse;
import com.example.demo.pets.models.Favorite;
import com.example.demo.pets.models.Pet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class FavoriteService {

  private final FavoriteRepository favoriteRepository;
  private final PetRepository petRepository;

  @Transactional
  public void addFavorite(Long petId, User user) {
    Pet pet =
        petRepository
            .findById(petId)
            .orElseThrow(() -> new IllegalArgumentException("Pet not found"));

    if (favoriteRepository.existsByUserAndPet(user, pet)) {
      throw new IllegalArgumentException("Pet is already in favourites");
    }

    Favorite favorite = Favorite.builder().user(user).pet(pet).build();
    favoriteRepository.save(favorite);
    log.info("User '{}' added pet '{}' to favourites", user.getUsername(), pet.getName());
  }

  @Transactional
  public void removeFavorite(Long petId, User user) {
    Pet pet =
        petRepository
            .findById(petId)
            .orElseThrow(() -> new IllegalArgumentException("Pet not found"));

    Favorite favorite =
        favoriteRepository
            .findByUserAndPet(user, pet)
            .orElseThrow(() -> new IllegalArgumentException("Pet is not in favourites"));

    favoriteRepository.delete(favorite);
    log.info("User '{}' removed pet '{}' from favourites", user.getUsername(), pet.getName());
  }

  @Transactional(readOnly = true)
  public List<PetPreviewResponse> getUserFavorites(User user) {
    return favoriteRepository.findByUserOrderByCreatedAtDesc(user).stream()
        .map(favorite -> toPreviewResponse(favorite.getPet(), true))
        .toList();
  }

  @Transactional(readOnly = true)
  public boolean isFavorite(Long petId, User user) {
    Pet pet = petRepository.findById(petId).orElse(null);
    if (pet == null) {
      return false;
    }
    return favoriteRepository.existsByUserAndPet(user, pet);
  }

  @Transactional(readOnly = true)
  public Set<Long> getUserFavoritePetIds(User user) {
    return favoriteRepository.findPetIdsByUser(user);
  }

  private PetPreviewResponse toPreviewResponse(Pet pet, boolean isFavorited) {
    return PetPreviewResponse.builder()
        .id(pet.getId())
        .name(pet.getName())
        .species(pet.getSpecies())
        .breed(pet.getBreed())
        .imageUrl(pet.getImageUrl())
        .isFavorited(isFavorited)
        .build();
  }
}
