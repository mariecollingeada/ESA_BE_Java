package com.example.demo.pets;

import com.example.demo.auth.models.User;
import com.example.demo.pets.dto.PetPreviewResponse;
import com.example.demo.pets.dto.PetRequest;
import com.example.demo.pets.dto.PetResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PetService {

  private final PetRepository petRepository;

  public List<PetPreviewResponse> getAllPetPreviews() {
    return petRepository.findAllByOrderByCreatedAtDesc().stream()
        .map(
            pet ->
                PetPreviewResponse.builder()
                    .id(pet.getId())
                    .name(pet.getName())
                    .species(pet.getSpecies())
                    .imageUrl(pet.getImageUrl())
                    .build())
        .toList();
  }

  public List<PetResponse> getAllPets() {
    return petRepository.findAllByOrderByCreatedAtDesc().stream()
        .map(PetResponse::fromEntity)
        .toList();
  }

  public List<PetResponse> getPetsByUser(Long userId) {
    return petRepository.findByUserId(userId).stream().map(PetResponse::fromEntity).toList();
  }

  public PetResponse getPetById(Long id) {
    Pet pet =
        petRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Pet not found"));
    return PetResponse.fromEntity(pet);
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
        petRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Pet not found"));

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
        petRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Pet not found"));

    if (!pet.getUser().getId().equals(user.getId())) {
      throw new IllegalArgumentException("You can only delete your own pets");
    }

    petRepository.delete(pet);
    log.info("Deleted pet '{}' for user '{}'", pet.getName(), user.getUsername());
  }
}
