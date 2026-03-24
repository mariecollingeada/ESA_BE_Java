package com.example.demo.pets.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Species {
  DOG("Dog"),
  CAT("Cat"),
  BIRD("Bird"),
  RABBIT("Rabbit"),
  FISH("Fish"),
  REPTILE("Reptile"),
  SMALL_MAMMAL("Small Mammal"),
  OTHER("Other");

  private final String displayName;

  Species(String displayName) {
    this.displayName = displayName;
  }

  @JsonValue
  public String getDisplayName() {
    return displayName;
  }

  @JsonCreator
  public static Species fromDisplayName(String value) {
    for (Species species : Species.values()) {
      if (species.displayName.equalsIgnoreCase(value)) {
        return species;
      }
    }
    throw new IllegalArgumentException("Unknown species: " + value);
  }
}
