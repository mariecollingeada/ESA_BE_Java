package com.example.demo.pets;

import com.example.demo.auth.models.User;
import com.example.demo.pets.models.Favorite;
import com.example.demo.pets.models.Pet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

  Optional<Favorite> findByUserAndPet(User user, Pet pet);

  boolean existsByUserAndPet(User user, Pet pet);

  List<Favorite> findByUserOrderByCreatedAtDesc(User user);

  void deleteByUserAndPet(User user, Pet pet);

  @Query("SELECT f.pet.id FROM Favorite f WHERE f.user = :user")
  Set<Long> findPetIdsByUser(@Param("user") User user);

  @Query("SELECT f.pet.id, COUNT(f) FROM Favorite f GROUP BY f.pet.id")
  List<Object[]> countFavoritesByPetId();
}
