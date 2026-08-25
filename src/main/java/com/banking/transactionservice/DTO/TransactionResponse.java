package com.banking.transactionservice.DTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.banking.transactionservice.Entity.TransactionStatus;
import com.banking.transactionservice.Entity.TransactionType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TransactionResponse {

  private String id;
  private String senderAccountNumber;

  private String receiverAccountNumber;

  private BigDecimal amount;

  private TransactionType type;

  private TransactionStatus status;
  private String description;
  private String failureReason;
  private String referenceNumber;
  private LocalDateTime createdAt;
  private LocalDateTime completedAt;

}
