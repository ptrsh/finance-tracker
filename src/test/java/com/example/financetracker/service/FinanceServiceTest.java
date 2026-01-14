package com.example.financetracker.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.financetracker.dto.*;
import com.example.financetracker.entity.*;
import com.example.financetracker.repository.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FinanceServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private WalletRepository walletRepository;
  @Mock private CategoryRepository categoryRepository;
  @Mock private TransactionRepository transactionRepository;

  @InjectMocks private FinanceService financeService;

  private User user;
  private Wallet wallet;

  @BeforeEach
  void setUp() {
    wallet = Wallet.builder().id(1L).balance(new BigDecimal("1000")).build();
    user = User.builder().id(1L).username("test").wallet(wallet).build();
  }

  // ... (Тесты 1-7 оставляем без изменений, они работают)

  // ИСПРАВЛЕННЫЙ ТЕСТ №8
  @Test
  void addTransaction_BudgetExceeded_ReturnsWarning() {
    Category cat =
        Category.builder()
            .id(1L)
            .name("Food")
            .type(TransactionType.EXPENSE)
            .budgetLimit(new BigDecimal("500"))
            .build();

    when(userRepository.findByUsername("test")).thenReturn(Optional.of(user));
    when(categoryRepository.findByUser(user)).thenReturn(List.of(cat));

    // Существующая транзакция (400)
    Transaction existing =
        Transaction.builder().category(cat).amount(new BigDecimal("400")).build();

    // Новая транзакция (200), которую мы сейчас добавляем.
    // В реальной базе она бы сохранилась и вернулась в поиске.
    // В Mockito мы должны явно сказать: "верни список из двух элементов".
    Transaction newTrans =
        Transaction.builder().category(cat).amount(new BigDecimal("200")).build();

    // Важное изменение здесь: возвращаем List.of(existing, newTrans)
    when(transactionRepository.findByWallet(wallet)).thenReturn(List.of(existing, newTrans));

    TransactionDto dto = new TransactionDto();
    dto.setCategoryName("Food");
    dto.setAmount(new BigDecimal("200")); // 400 + 200 = 600 > 500

    TransactionResponse response = financeService.addTransaction("test", dto);

    // Теперь сервис посчитает сумму как 600 и выдаст WARNING
    assertNotNull(response.getMessage());
    assertTrue(response.getMessage().contains("WARNING"));
  }

  // ... (Остальные тесты 9-15 оставляем без изменений, скопируйте их из предыдущего ответа)

  // Для удобства дублирую остальные тесты, чтобы файл был полным:

  @Test
  void createCategory_Success() {
    when(userRepository.findByUsername("test")).thenReturn(Optional.of(user));
    when(categoryRepository.findByNameAndUserAndType(any(), any(), any()))
        .thenReturn(Optional.empty());
    when(categoryRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

    CategoryDto dto = new CategoryDto();
    dto.setName("Food");
    dto.setType(TransactionType.EXPENSE);

    Category result = financeService.createCategory("test", dto);
    assertEquals("Food", result.getName());
  }

  @Test
  void createCategory_Duplicate_ThrowsException() {
    when(userRepository.findByUsername("test")).thenReturn(Optional.of(user));
    when(categoryRepository.findByNameAndUserAndType(any(), any(), any()))
        .thenReturn(Optional.of(new Category()));

    CategoryDto dto = new CategoryDto();
    dto.setName("Food");
    dto.setType(TransactionType.EXPENSE);

    assertThrows(RuntimeException.class, () -> financeService.createCategory("test", dto));
  }

  @Test
  void updateCategory_Success() {
    Category category = Category.builder().id(1L).user(user).name("Old").build();
    when(userRepository.findByUsername("test")).thenReturn(Optional.of(user));
    when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
    when(categoryRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

    CategoryDto dto = new CategoryDto();
    dto.setName("New");

    Category result = financeService.updateCategory("test", 1L, dto);
    assertEquals("New", result.getName());
  }

  @Test
  void updateCategory_AccessDenied() {
    User otherUser = User.builder().id(2L).build();
    Category category = Category.builder().id(1L).user(otherUser).name("Old").build();

    when(userRepository.findByUsername("test")).thenReturn(Optional.of(user));
    when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

    assertThrows(
        RuntimeException.class, () -> financeService.updateCategory("test", 1L, new CategoryDto()));
  }

  @Test
  void addTransaction_Income_IncreasesBalance() {
    Category cat = Category.builder().name("Salary").type(TransactionType.INCOME).build();
    when(userRepository.findByUsername("test")).thenReturn(Optional.of(user));
    when(categoryRepository.findByUser(user)).thenReturn(List.of(cat));

    TransactionDto dto = new TransactionDto();
    dto.setCategoryName("Salary");
    dto.setAmount(new BigDecimal("500"));

    financeService.addTransaction("test", dto);
    assertEquals(new BigDecimal("1500"), wallet.getBalance());
  }

  @Test
  void addTransaction_Expense_DecreasesBalance() {
    Category cat = Category.builder().id(1L).name("Food").type(TransactionType.EXPENSE).build();
    when(userRepository.findByUsername("test")).thenReturn(Optional.of(user));
    when(categoryRepository.findByUser(user)).thenReturn(List.of(cat));

    TransactionDto dto = new TransactionDto();
    dto.setCategoryName("Food");
    dto.setAmount(new BigDecimal("100"));

    financeService.addTransaction("test", dto);
    assertEquals(new BigDecimal("900"), wallet.getBalance());
  }

  @Test
  void addTransaction_Expense_InsufficientFunds() {
    Category cat = Category.builder().name("Food").type(TransactionType.EXPENSE).build();
    when(userRepository.findByUsername("test")).thenReturn(Optional.of(user));
    when(categoryRepository.findByUser(user)).thenReturn(List.of(cat));

    TransactionDto dto = new TransactionDto();
    dto.setCategoryName("Food");
    dto.setAmount(new BigDecimal("2000"));

    assertThrows(RuntimeException.class, () -> financeService.addTransaction("test", dto));
  }

  @Test
  void addTransaction_CategoryNotFound() {
    when(userRepository.findByUsername("test")).thenReturn(Optional.of(user));
    when(categoryRepository.findByUser(user)).thenReturn(Collections.emptyList());

    TransactionDto dto = new TransactionDto();
    dto.setCategoryName("Unknown");
    dto.setAmount(BigDecimal.TEN);

    assertThrows(RuntimeException.class, () -> financeService.addTransaction("test", dto));
  }

  @Test
  void transfer_Success() {
    User receiver =
        User.builder()
            .id(2L)
            .username("bob")
            .wallet(Wallet.builder().balance(BigDecimal.ZERO).build())
            .build();

    when(userRepository.findByUsername("test")).thenReturn(Optional.of(user));
    when(userRepository.findByUsername("bob")).thenReturn(Optional.of(receiver));
    when(categoryRepository.findByNameAndUserAndType(any(), any(), any()))
        .thenReturn(Optional.empty());

    TransferRequest req = new TransferRequest();
    req.setReceiverUsername("bob");
    req.setAmount(new BigDecimal("100"));

    financeService.transfer("test", req);

    assertEquals(new BigDecimal("900"), wallet.getBalance());
    assertEquals(new BigDecimal("100"), receiver.getWallet().getBalance());
  }

  @Test
  void transfer_Self_ThrowsException() {
    when(userRepository.findByUsername("test")).thenReturn(Optional.of(user));
    when(userRepository.findByUsername("test_receiver")).thenReturn(Optional.of(user));

    TransferRequest req = new TransferRequest();
    req.setReceiverUsername("test_receiver");

    assertThrows(RuntimeException.class, () -> financeService.transfer("test", req));
  }

  @Test
  void transfer_ReceiverNotFound() {
    when(userRepository.findByUsername("test")).thenReturn(Optional.of(user));
    when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

    TransferRequest req = new TransferRequest();
    req.setReceiverUsername("ghost");

    assertThrows(RuntimeException.class, () -> financeService.transfer("test", req));
  }

  @Test
  void getStats_CalculatesCorrectly() {
    Category inc = Category.builder().name("Job").type(TransactionType.INCOME).build();
    Category exp =
        Category.builder()
            .name("Food")
            .type(TransactionType.EXPENSE)
            .budgetLimit(BigDecimal.TEN)
            .build();

    Transaction t1 =
        Transaction.builder()
            .category(inc)
            .amount(new BigDecimal("100"))
            .date(LocalDateTime.now())
            .build();
    Transaction t2 =
        Transaction.builder()
            .category(exp)
            .amount(new BigDecimal("50"))
            .date(LocalDateTime.now())
            .build();

    when(userRepository.findByUsername("test")).thenReturn(Optional.of(user));
    when(transactionRepository.findByWallet(wallet)).thenReturn(List.of(t1, t2));
    when(categoryRepository.findByUser(user)).thenReturn(List.of(inc, exp));

    StatsResponse stats = financeService.getStats("test", null, null);

    assertEquals(new BigDecimal("100"), stats.getTotalIncome());
    assertEquals(new BigDecimal("50"), stats.getTotalExpense());
  }

  @Test
  void export_ReturnsList() {
    Category cat = Category.builder().name("Food").build();
    Transaction t1 =
        Transaction.builder()
            .id(1L)
            .category(cat)
            .amount(BigDecimal.TEN)
            .date(LocalDateTime.now())
            .build();

    when(userRepository.findByUsername("test")).thenReturn(Optional.of(user));
    when(transactionRepository.findByWallet(wallet)).thenReturn(List.of(t1));

    List<TransactionDto> result = financeService.exportTransactions("test");
    assertEquals(1, result.size());
    assertEquals(BigDecimal.TEN, result.get(0).getAmount());
  }

  @Test
  void getUser_NotFound() {
    when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());
    assertThrows(RuntimeException.class, () -> financeService.getCategories("unknown"));
  }
}
