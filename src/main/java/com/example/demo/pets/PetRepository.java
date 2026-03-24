package com.example.demo.pets;

import com.example.demo.pets.models.Pet;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PetRepository extends JpaRepository<Pet, Long> {

  List<Pet> findByUserId(Long userId);

  List<Pet> findAllByOrderByCreatedAtDesc();
}
