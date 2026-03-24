package com.example.demo.pets;

import com.example.demo.auth.models.User;
import com.example.demo.pets.dto.PetPreviewResponse;
import com.example.demo.pets.dto.PetRequest;
import com.example.demo.pets.dto.PetResponse;
import com.example.demo.pets.models.Pet;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
@RequiredArgsConstructor
public class PetService {

  private final PetRepository petRepository;
  private final CloudinaryService cloudinaryService;
  private final FavoriteRepository favoriteRepository;

  private static final String PET_NOT_FOUND = "Pet not found";

  public List<PetPreviewResponse> getAllPetPreviews() {
    return getAllPetPreviews(null);
  }

  public List<PetPreviewResponse> getAllPetPreviews(User user) {
    Set<Long> favoritePetIds = user != null ? favoriteRepository.findPetIdsByUser(user) : Set.of();
    Map<Long, Long> favouriteCounts = getFavouriteCountsMap();
    return petRepository.findAllByOrderByCreatedAtDesc().stream()
        .map(
            pet ->
                toPreviewResponse(
                    pet,
                    favoritePetIds.contains(pet.getId()),
                    favouriteCounts.getOrDefault(pet.getId(), 0L)))
        .toList();
  }

  private Map<Long, Long> getFavouriteCountsMap() {
    Map<Long, Long> counts = new HashMap<>();
    for (Object[] row : favoriteRepository.countFavoritesByPetId()) {
      counts.put((Long) row[0], (Long) row[1]);
    }
    return counts;
  }

  private PetPreviewResponse toPreviewResponse(Pet pet, Boolean isFavorited, Long favouriteCount) {
    return PetPreviewResponse.builder()
        .id(pet.getId())
        .name(pet.getName())
        .species(pet.getSpecies())
        .breed(pet.getBreed())
        .imageUrl(pet.getImageUrl())
        .isFavorited(isFavorited)
        .favouriteCount(favouriteCount)
        .build();
  }

  @Transactional(readOnly = true)
  public List<PetResponse> getAllPets() {
    return petRepository.findAllByOrderByCreatedAtDesc().stream()
        .map(PetResponse::fromEntity)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<PetResponse> getPetsByUser(Long userId) {
    return petRepository.findByUserId(userId).stream().map(PetResponse::fromEntity).toList();
  }

  @Transactional(readOnly = true)
  public PetResponse getPetById(Long id) {
    return getPetById(id, null);
  }

  @Transactional(readOnly = true)
  public PetResponse getPetById(Long id, User user) {
    Pet pet =
        petRepository.findById(id).orElseThrow(() -> new IllegalArgumentException(PET_NOT_FOUND));
    Boolean isFavorited = user != null ? favoriteRepository.existsByUserAndPet(user, pet) : null;
    return PetResponse.fromEntity(pet, isFavorited);
  }

  @Transactional
  public PetResponse createPet(PetRequest request, User user) {
    Pet pet =
        Pet.builder()
            .name(request.getName())
            .species(request.getSpecies())
            .breed(request.getBreed())
            .age(request.getAge())
            .description(request.getDescription())
            .user(user)
            .build();

    Pet savedPet = petRepository.save(pet);
    log.info("Created pet '{}' for user '{}'", savedPet.getName(), user.getUsername());
    return PetResponse.fromEntity(savedPet);
  }

  @Transactional
  public PetResponse updatePet(Long id, PetRequest request, User user) {
    Pet pet =
        petRepository.findById(id).orElseThrow(() -> new IllegalArgumentException(PET_NOT_FOUND));

    if (!pet.getUser().getId().equals(user.getId())) {
      throw new IllegalArgumentException("You can only update your own pets");
    }

    pet.setName(request.getName());
    pet.setSpecies(request.getSpecies());
    pet.setBreed(request.getBreed());
    pet.setAge(request.getAge());
    pet.setDescription(request.getDescription());

    Pet savedPet = petRepository.save(pet);
    log.info("Updated pet '{}' for user '{}'", savedPet.getName(), user.getUsername());
    return PetResponse.fromEntity(savedPet);
  }

  @Transactional
  public void deletePet(Long id, User user) {
    Pet pet =
        petRepository.findById(id).orElseThrow(() -> new IllegalArgumentException(PET_NOT_FOUND));

    if (!pet.getUser().getId().equals(user.getId())) {
      throw new IllegalArgumentException("You can only delete your own pets");
    }

    petRepository.delete(pet);
    log.info("Deleted pet '{}' for user '{}'", pet.getName(), user.getUsername());
  }

  @Transactional
  public PetResponse uploadPetImage(Long id, MultipartFile file, User user) {
    Pet pet =
        petRepository.findById(id).orElseThrow(() -> new IllegalArgumentException(PET_NOT_FOUND));

    if (!pet.getUser().getId().equals(user.getId())) {
      throw new IllegalArgumentException("You can only update your own pets");
    }

    try {
      String imageUrl = cloudinaryService.uploadImage(file);
      pet.setImageUrl(imageUrl);
      Pet savedPet = petRepository.save(pet);
      log.info("Uploaded image for pet '{}' for user '{}'", savedPet.getName(), user.getUsername());
      return PetResponse.fromEntity(savedPet);
    } catch (IOException e) {
      log.error("Failed to upload image to Cloudinary", e);
      throw new RuntimeException("Failed to upload image", e);
    }
  }
}
