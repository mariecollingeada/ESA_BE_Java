package com.example.demo.pets.dto;

import com.example.demo.pets.models.Pet;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PetResponse {
  private Long id;
  private String name;
  private String species;
  private String breed;
  private Integer age;
  private String description;
  private String imageUrl;
  private String ownerUsername;
  private Long ownerId;

  public static PetResponse fromEntity(Pet pet) {
    return PetResponse.builder()
        .id(pet.getId())
        .name(pet.getName())
        .species(pet.getSpecies())
        .breed(pet.getBreed())
        .age(pet.getAge())
        .description(pet.getDescription())
        .imageUrl(pet.getImageUrl())
        .ownerUsername(pet.getUser().getUsername())
        .ownerId(pet.getUser().getId())
        .build();
  }
}
