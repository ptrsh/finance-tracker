package com.example.financetracker.service;

import com.example.financetracker.dto.CategoryDto;
import com.example.financetracker.dto.StatsResponse;
import com.example.financetracker.dto.TransactionDto;
import com.example.financetracker.dto.TransactionResponse;
import com.example.financetracker.dto.TransferRequest;
import com.example.financetracker.entity.Category;
import com.example.financetracker.entity.Transaction;
import com.example.financetracker.entity.TransactionType;
import com.example.financetracker.entity.User;
import com.example.financetracker.entity.Wallet;
import com.example.financetracker.repository.CategoryRepository;
import com.example.financetracker.repository.TransactionRepository;
import com.example.financetracker.repository.UserRepository;
import com.example.financetracker.repository.WalletRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FinanceService {

  private final UserRepository userRepository;
  private final WalletRepository walletRepository;
  private final CategoryRepository categoryRepository;
  private final TransactionRepository transactionRepository;

  private static final String TRANSFER_CATEGORY_NAME = "Переводы";

  // Категории (Создание + Редактирование)
  public Category createCategory(String username, CategoryDto dto) {
    User user = getUser(username);
    if (categoryRepository
        .findByNameAndUserAndType(dto.getName(), user, dto.getType())
        .isPresent()) {
      throw new RuntimeException("Category already exists");
    }
    Category category =
        Category.builder()
            .name(dto.getName())
            .type(dto.getType())
            .budgetLimit(dto.getBudgetLimit())
            .user(user)
            .build();
    return categoryRepository.save(category);
  }

  // Редактирование бюджета и имени
  @Transactional
  public Category updateCategory(String username, Long categoryId, CategoryDto dto) {
    User user = getUser(username);
    Category category =
        categoryRepository
            .findById(categoryId)
            .orElseThrow(() -> new RuntimeException("Category not found"));

    if (!category.getUser().getId().equals(user.getId())) {
      throw new RuntimeException("Access denied");
    }

    category.setName(dto.getName());
    category.setBudgetLimit(dto.getBudgetLimit());
    return categoryRepository.save(category);
  }

  public List<Category> getCategories(String username) {
    return categoryRepository.findByUser(getUser(username));
  }

  // Транзакции
  @Transactional
  public TransactionResponse addTransaction(String username, TransactionDto dto) {
    User user = getUser(username);
    Wallet wallet = user.getWallet();

    List<Category> categories =
        categoryRepository.findByUser(user).stream()
            .filter(c -> c.getName().equals(dto.getCategoryName()))
            .toList();

    if (categories.isEmpty()) {
      throw new RuntimeException("Category not found: " + dto.getCategoryName());
    }
    Category category = categories.get(0);

    BigDecimal amount = dto.getAmount();

    if (category.getType() == TransactionType.EXPENSE) {
      if (wallet.getBalance().compareTo(amount) < 0) {
        throw new RuntimeException("Insufficient funds");
      }
      wallet.setBalance(wallet.getBalance().subtract(amount));
    } else {
      wallet.setBalance(wallet.getBalance().add(amount));
    }

    Transaction transaction =
        Transaction.builder()
            .wallet(wallet)
            .category(category)
            .amount(amount)
            .description(dto.getDescription())
            .date(dto.getDate() != null ? dto.getDate() : LocalDateTime.now())
            .build();
    transactionRepository.save(transaction);

    return buildResponse(wallet, category, dto);
  }

  // Переводы
  @Transactional
  public void transfer(String senderName, TransferRequest request) {
    User sender = getUser(senderName);
    User receiver =
        userRepository
            .findByUsername(request.getReceiverUsername())
            .orElseThrow(() -> new RuntimeException("Receiver not found"));

    if (sender.getId().equals(receiver.getId())) {
      throw new RuntimeException("Cannot transfer to self");
    }

    Wallet senderWallet = sender.getWallet();
    Wallet receiverWallet = receiver.getWallet();
    BigDecimal amount = request.getAmount();

    if (senderWallet.getBalance().compareTo(amount) < 0) {
      throw new RuntimeException("Insufficient funds");
    }

    senderWallet.setBalance(senderWallet.getBalance().subtract(amount));
    Category senderCategory = getOrCreateTransferCategory(sender, TransactionType.EXPENSE);

    transactionRepository.save(
        Transaction.builder()
            .wallet(senderWallet)
            .category(senderCategory)
            .amount(amount)
            .description("Transfer to " + receiver.getUsername())
            .date(LocalDateTime.now())
            .build());

    receiverWallet.setBalance(receiverWallet.getBalance().add(amount));
    Category receiverCategory = getOrCreateTransferCategory(receiver, TransactionType.INCOME);

    transactionRepository.save(
        Transaction.builder()
            .wallet(receiverWallet)
            .category(receiverCategory)
            .amount(amount)
            .description("Transfer from " + sender.getUsername())
            .date(LocalDateTime.now())
            .build());
  }

  private Category getOrCreateTransferCategory(User user, TransactionType type) {
    return categoryRepository
        .findByNameAndUserAndType(TRANSFER_CATEGORY_NAME, user, type)
        .orElseGet(
            () ->
                categoryRepository.save(
                    Category.builder().name(TRANSFER_CATEGORY_NAME).user(user).type(type).build()));
  }

  // Статистика (Фильтрация)
  public StatsResponse getStats(String username, LocalDateTime from, LocalDateTime to) {
    User user = getUser(username);
    // Фильтруем транзакции по дате (если даты переданы)
    List<Transaction> transactions =
        transactionRepository.findByWallet(user.getWallet()).stream()
            .filter(
                t ->
                    (from == null || !t.getDate().isBefore(from))
                        && (to == null || !t.getDate().isAfter(to)))
            .toList();

    BigDecimal totalIncome = BigDecimal.ZERO;
    BigDecimal totalExpense = BigDecimal.ZERO;
    Map<String, BigDecimal> expensesByCategory = new HashMap<>();

    for (Transaction t : transactions) {
      if (t.getCategory().getType() == TransactionType.INCOME) {
        totalIncome = totalIncome.add(t.getAmount());
      } else {
        totalExpense = totalExpense.add(t.getAmount());
        expensesByCategory.merge(t.getCategory().getName(), t.getAmount(), BigDecimal::add);
      }
    }

    Map<String, BigDecimal> budgetStatus = new HashMap<>();
    List<Category> categories = categoryRepository.findByUser(user);
    for (Category c : categories) {
      if (c.getType() == TransactionType.EXPENSE && c.getBudgetLimit() != null) {
        // Бюджет считаем по тратам за выбранный период (или за всё время)
        BigDecimal spent = expensesByCategory.getOrDefault(c.getName(), BigDecimal.ZERO);
        budgetStatus.put(c.getName(), c.getBudgetLimit().subtract(spent));
      }
    }

    StatsResponse stats = new StatsResponse();
    stats.setTotalIncome(totalIncome);
    stats.setTotalExpense(totalExpense);
    stats.setExpensesByCategory(expensesByCategory);
    stats.setBudgetStatus(budgetStatus);
    return stats;
  }

  // Экспорт данных
  public List<TransactionDto> exportTransactions(String username) {
    User user = getUser(username);
    return transactionRepository.findByWallet(user.getWallet()).stream()
        .map(
            t -> {
              TransactionDto dto = new TransactionDto();
              dto.setId(t.getId());
              dto.setCategoryName(t.getCategory().getName());
              dto.setAmount(t.getAmount());
              dto.setDescription(t.getDescription());
              dto.setDate(t.getDate());
              return dto;
            })
        .collect(Collectors.toList());
  }

  private TransactionResponse buildResponse(Wallet wallet, Category category, TransactionDto dto) {
    String message = "Success";
    BigDecimal remaining = null;

    if (category.getType() == TransactionType.EXPENSE && category.getBudgetLimit() != null) {
      BigDecimal totalSpentInCategory =
          transactionRepository.findByWallet(wallet).stream()
              .filter(t -> t.getCategory().getId().equals(category.getId()))
              .map(Transaction::getAmount)
              .reduce(BigDecimal.ZERO, BigDecimal::add);

      remaining = category.getBudgetLimit().subtract(totalSpentInCategory);
      if (remaining.compareTo(BigDecimal.ZERO) < 0) {
        message = "WARNING: Budget exceeded for category " + category.getName();
      }
    }

    TransactionResponse response = new TransactionResponse();
    response.setMessage(message);
    response.setRemainingBudget(remaining);
    response.setTransaction(dto);
    return response;
  }

  private User getUser(String username) {
    return userRepository
        .findByUsername(username)
        .orElseThrow(() -> new RuntimeException("User not found"));
  }
}
