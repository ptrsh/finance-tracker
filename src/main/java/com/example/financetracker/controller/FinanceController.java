package com.example.financetracker.controller;

import com.example.financetracker.dto.*;
import com.example.financetracker.entity.Category;
import com.example.financetracker.service.FinanceService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/finance")
@RequiredArgsConstructor
public class FinanceController {

  private final FinanceService financeService;

  @PostMapping("/categories")
  public ResponseEntity<Category> createCategory(
      @RequestBody @Valid CategoryDto dto, Authentication authentication) {
    return ResponseEntity.ok(financeService.createCategory(authentication.getName(), dto));
  }

  @PutMapping("/categories/{id}")
  public ResponseEntity<Category> updateCategory(
      @PathVariable Long id, @RequestBody @Valid CategoryDto dto, Authentication authentication) {
    return ResponseEntity.ok(financeService.updateCategory(authentication.getName(), id, dto));
  }

  @GetMapping("/categories")
  public ResponseEntity<List<Category>> getCategories(Authentication authentication) {
    return ResponseEntity.ok(financeService.getCategories(authentication.getName()));
  }

  @PostMapping("/transactions")
  public ResponseEntity<TransactionResponse> addTransaction(
      @RequestBody @Valid TransactionDto dto, Authentication authentication) {
    return ResponseEntity.ok(financeService.addTransaction(authentication.getName(), dto));
  }

  @GetMapping("/stats")
  public ResponseEntity<StatsResponse> getStats(
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to,
      Authentication authentication) {
    return ResponseEntity.ok(
        financeService.getStats(
            authentication.getName(),
            from != null ? from.atStartOfDay() : null,
            to != null ? to.atTime(23, 59, 59) : null));
  }

  @PostMapping("/transfer")
  public ResponseEntity<String> transfer(
      @RequestBody @Valid TransferRequest request, Authentication authentication) {
    financeService.transfer(authentication.getName(), request);
    return ResponseEntity.ok("Transfer successful");
  }

  @GetMapping("/export")
  public ResponseEntity<List<TransactionDto>> export(Authentication authentication) {
    return ResponseEntity.ok(financeService.exportTransactions(authentication.getName()));
  }
}
