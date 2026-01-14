package com.example.financetracker.repository;

import com.example.financetracker.entity.User;
import com.example.financetracker.entity.Wallet;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
  Optional<Wallet> findByUser(User user);
}
