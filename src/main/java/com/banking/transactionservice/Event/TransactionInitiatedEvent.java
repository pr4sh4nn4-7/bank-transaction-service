package com.banking.transactionservice.Event;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionInitiatedEvent {
  private String transactionId;
  private String senderAccountNumber;
  private String receiverAccountNumber;
  private BigDecimal amount;
  private String description;

}
