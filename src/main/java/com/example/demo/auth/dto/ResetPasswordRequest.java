package com.example.demo.auth.dto;

import lombok.Data;

@Data
public class ResetPasswordRequest {
  private String email;
  private String token;
  private String newPassword;
  private String confirmNewPassword;
}
