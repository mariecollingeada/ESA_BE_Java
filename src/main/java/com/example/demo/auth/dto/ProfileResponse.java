package com.example.demo.auth.dto;

import java.util.Set;

public record ProfileResponse(Long id, String username, String email, Set<String> roles) {}
