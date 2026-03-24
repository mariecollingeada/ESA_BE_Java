package com.example.demo.pets.dto;

import com.example.demo.pets.models.Species;
import lombok.Data;

@Data
public class PetRequest {
  private String name;
  private Species species;
  private String breed;
  private Integer age;
  private String description;
}
