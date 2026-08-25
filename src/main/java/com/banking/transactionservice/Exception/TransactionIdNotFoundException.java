package com.banking.transactionservice.Exception;

public class TransactionIdNotFoundException extends RuntimeException {

  public TransactionIdNotFoundException(String message) {
    super(message);
  }

}
