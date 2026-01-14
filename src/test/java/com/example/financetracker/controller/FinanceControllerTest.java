package com.example.financetracker.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.financetracker.repository.CategoryRepository;
import com.example.financetracker.repository.TransactionRepository;
import com.example.financetracker.repository.UserRepository;
import com.example.financetracker.repository.WalletRepository;
import com.example.financetracker.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class FinanceControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private AuthService authService;

  // Внедряем репозитории для очистки базы
  @Autowired private UserRepository userRepository;
  @Autowired private CategoryRepository categoryRepository;
  @Autowired private TransactionRepository transactionRepository;
  @Autowired private WalletRepository walletRepository;

  @BeforeEach
  void setUp() {
    // 1. Полная очистка базы перед каждым тестом
    // Порядок важен из-за внешних ключей!
    transactionRepository.deleteAll();
    categoryRepository.deleteAll();
    // Wallet удалится каскадно при удалении User, либо можно явно: walletRepository.deleteAll();
    userRepository.deleteAll();

    // 2. Создаем чистого пользователя
    authService.register("user1", "pass");
  }

  @Test
  @WithMockUser(username = "user1")
  void createCategory_ShouldReturn200() throws Exception {
    String json =
        """
            {
                "name": "Groceries",
                "type": "EXPENSE",
                "budgetLimit": 5000
            }
        """;

    mockMvc
        .perform(
            post("/api/finance/categories").contentType(MediaType.APPLICATION_JSON).content(json))
        .andExpect(status().isOk());
  }

  @Test
  void createCategory_Unauthorized_ShouldReturn401() throws Exception {
    mockMvc
        .perform(
            post("/api/finance/categories").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isUnauthorized());
  }
}
