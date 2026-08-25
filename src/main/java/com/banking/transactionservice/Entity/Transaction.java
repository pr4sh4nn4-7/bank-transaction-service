package com.banking.transactionservice.Entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class Transaction {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;
  @Column(nullable = false)
  private String senderAccountNumber;

  @Column(nullable = false)
  private String receiverAccountNumber;

  @Column(nullable = false, precision = 15, scale = 2)

  private BigDecimal amount;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TransactionType type;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TransactionStatus status;
  private String description;
  private String failureReason;
  private String referenceNumber;
  @CreationTimestamp
  private LocalDateTime createdAt;
  private LocalDateTime completedAt;
}
