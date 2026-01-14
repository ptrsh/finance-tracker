package com.example.financetracker.repository;

import com.example.financetracker.entity.Category;
import com.example.financetracker.entity.TransactionType;
import com.example.financetracker.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
  List<Category> findByUser(User user);

  Optional<Category> findByNameAndUserAndType(String name, User user, TransactionType type);
}
