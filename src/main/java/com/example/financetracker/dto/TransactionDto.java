package com.example.financetracker.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class TransactionDto {
  private Long id;
  @NotBlank private String categoryName;

  @NotNull
  @Min(0)
  private BigDecimal amount;

  private String description;
  private LocalDateTime date;
}
