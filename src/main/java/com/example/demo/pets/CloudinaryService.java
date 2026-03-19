package com.example.demo.pets;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import java.io.IOException;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class CloudinaryService {

  private final Cloudinary cloudinary;

  public CloudinaryService(@Value("${cloudinary-url}") String cloudinaryUrl) {
    this.cloudinary = new Cloudinary(cloudinaryUrl);
    log.info("Cloudinary configured");
  }

  public String uploadImage(MultipartFile file) throws IOException {
    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("File is empty");
    }

    Map<?, ?> uploadResult =
        cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap("folder", "pets"));

    String url = (String) uploadResult.get("secure_url");
    log.info("Uploaded image to Cloudinary: {}", url);
    return url;
  }

  public void deleteImage(String publicId) throws IOException {
    cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
    log.info("Deleted image from Cloudinary: {}", publicId);
  }
}
