package com.example.financetracker.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class TransactionResponse {
  private String message;
  private BigDecimal remainingBudget;
  private TransactionDto transaction;
}
