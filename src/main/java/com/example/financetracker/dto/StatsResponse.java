package com.example.financetracker.dto;

import java.math.BigDecimal;
import java.util.Map;
import lombok.Data;

@Data
public class StatsResponse {
  private BigDecimal totalIncome;
  private BigDecimal totalExpense;
  private Map<String, BigDecimal> expensesByCategory;
  private Map<String, BigDecimal> budgetStatus;
}
