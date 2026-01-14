package com.example.financetracker.service;

import com.example.financetracker.entity.User;
import com.example.financetracker.entity.Wallet;
import com.example.financetracker.repository.UserRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @Transactional
  public void register(String username, String password) {
    if (userRepository.findByUsername(username).isPresent()) {
      throw new RuntimeException("Username already exists");
    }

    User user =
        User.builder().username(username).password(passwordEncoder.encode(password)).build();

    Wallet wallet = Wallet.builder().user(user).balance(BigDecimal.ZERO).build();

    user.setWallet(wallet);
    userRepository.save(user);
  }
}
