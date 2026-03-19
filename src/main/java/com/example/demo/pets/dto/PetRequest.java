package com.example.demo.pets.dto;

import lombok.Data;

@Data
public class PetRequest {
  private String name;
  private String species;
  private String breed;
  private Integer age;
  private String description;
}
