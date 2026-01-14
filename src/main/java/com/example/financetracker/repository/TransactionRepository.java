package com.example.financetracker.repository;

import com.example.financetracker.entity.Transaction;
import com.example.financetracker.entity.Wallet;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
  List<Transaction> findByWallet(Wallet wallet);
}
