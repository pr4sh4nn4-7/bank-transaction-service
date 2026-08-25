package com.banking.transactionservice.Exception;

public class AccountNumberNotFoundException extends RuntimeException {

  public AccountNumberNotFoundException(String message) {
    super(message);
  }

}
