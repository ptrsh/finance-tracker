package com.example.financetracker.dto;

import com.example.financetracker.entity.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class CategoryDto {
  private Long id;
  @NotBlank private String name;
  @NotNull private TransactionType type;
  private BigDecimal budgetLimit;
}
