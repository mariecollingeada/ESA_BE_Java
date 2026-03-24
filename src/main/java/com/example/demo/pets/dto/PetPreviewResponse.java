package com.example.demo.pets.dto;

import com.example.demo.pets.models.Species;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PetPreviewResponse {
  private Long id;
  private String name;
  private Species species;
  private String breed;
  private String imageUrl;
}
