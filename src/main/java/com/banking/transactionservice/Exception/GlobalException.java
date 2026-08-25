package com.banking.transactionservice.Exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalException {

  @ExceptionHandler({ TransactionIdNotFoundException.class, AccountNumberNotFoundException.class })
  public ResponseEntity<Map<String, Object>> transactionIdValidate(
      Exception ex

  ) {
    Map<String, Object> error = new HashMap();
    error.put("Status", HttpStatus.NOT_FOUND.value());
    error.put("error", ex.getMessage());
    error.put("timestamp", LocalDateTime.now());

    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);

  }

}
