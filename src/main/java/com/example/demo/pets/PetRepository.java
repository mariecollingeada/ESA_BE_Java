package com.example.demo.pets;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PetRepository extends JpaRepository<Pet, Long> {

  List<Pet> findByUserId(Long userId);

  List<Pet> findAllByOrderByCreatedAtDesc();
}
