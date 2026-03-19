package com.example.demo.pets;

import com.example.demo.auth.AuthService;
import com.example.demo.auth.models.User;
import com.example.demo.pets.dto.PetPreviewResponse;
import com.example.demo.pets.dto.PetRequest;
import com.example.demo.pets.dto.PetResponse;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/pets")
@RequiredArgsConstructor
public class PetController {

  private final PetService petService;
  private final AuthService authService;

  @GetMapping
  public ResponseEntity<List<PetPreviewResponse>> getAllPets() {
    return ResponseEntity.ok(petService.getAllPetPreviews());
  }

  @GetMapping("/{id}")
  public ResponseEntity<PetResponse> getPetById(@PathVariable Long id) {
    return ResponseEntity.ok(petService.getPetById(id));
  }

  @GetMapping("/my-pets")
  public ResponseEntity<List<PetResponse>> getMyPets(Principal principal) {
    User user = authService.findByUsername(principal.getName());
    return ResponseEntity.ok(petService.getPetsByUser(user.getId()));
  }

  @PostMapping("/create")
  public ResponseEntity<PetResponse> createPet(
      @RequestBody PetRequest request, Principal principal) {
    User user = authService.findByUsername(principal.getName());
    PetResponse pet = petService.createPet(request, user);
    return ResponseEntity.status(HttpStatus.CREATED).body(pet);
  }

  @PutMapping("/{id}")
  public ResponseEntity<PetResponse> updatePet(
      @PathVariable Long id, @RequestBody PetRequest request, Principal principal) {
    User user = authService.findByUsername(principal.getName());
    return ResponseEntity.ok(petService.updatePet(id, request, user));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deletePet(@PathVariable Long id, Principal principal) {
    User user = authService.findByUsername(principal.getName());
    petService.deletePet(id, user);
    return ResponseEntity.noContent().build();
  }

  @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<PetResponse> uploadPetImage(
      @PathVariable Long id, @RequestParam("file") MultipartFile file, Principal principal) {
    User user = authService.findByUsername(principal.getName());
    PetResponse pet = petService.uploadPetImage(id, file, user);
    return ResponseEntity.ok(pet);
  }
}
