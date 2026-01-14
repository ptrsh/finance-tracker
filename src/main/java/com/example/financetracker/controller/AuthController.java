package com.example.financetracker.controller;

import com.example.financetracker.dto.AuthRequest;
import com.example.financetracker.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
  private final AuthService authService;

  @PostMapping("/register")
  public ResponseEntity<String> register(@RequestBody @Valid AuthRequest request) {
    authService.register(request.getUsername(), request.getPassword());
    return ResponseEntity.ok("User registered successfully");
  }
}
