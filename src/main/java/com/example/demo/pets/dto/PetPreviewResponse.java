package com.example.demo.pets.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PetPreviewResponse {
  private Long id;
  private String name;
  private String species;
  private String imageUrl;
}
