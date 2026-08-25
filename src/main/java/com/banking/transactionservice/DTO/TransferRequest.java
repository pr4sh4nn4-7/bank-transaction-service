package com.banking.transactionservice.DTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.banking.transactionservice.Entity.TransactionStatus;
import com.banking.transactionservice.Entity.TransactionType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TransferRequest {

  @NotBlank(message = "senderAccountNumber is required")
  private String senderAccountNumber;

  @NotBlank(message = "receiverAccountNumber is required")
  private String receiverAccountNumber;

  @NotNull(message = "amount is required")
  @Positive(message = "amount is always Positive")
  private BigDecimal amount;

  private String description;

}
