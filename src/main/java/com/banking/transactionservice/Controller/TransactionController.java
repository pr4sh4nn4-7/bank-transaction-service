package com.banking.transactionservice.Controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.banking.transactionservice.DTO.TransactionResponse;
import com.banking.transactionservice.DTO.TransferRequest;
import com.banking.transactionservice.Service.TransactionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/transaction")
@RequiredArgsConstructor
@Slf4j
public class TransactionController {

  private final TransactionService transactionService;

  @PostMapping
  public ResponseEntity<TransactionResponse> Transfer(@Valid @RequestBody TransferRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.transfer(request));

  }

  @GetMapping("/{transactionId}")
  public ResponseEntity<TransactionResponse> getTransaction(
      @PathVariable UUID transactionId) {
    return ResponseEntity.ok(transactionService.getTransaction(transactionId.toString()));

  }

  @GetMapping("/account/{accountNumber}")
  public ResponseEntity<List<TransactionResponse>> getTransactionHistory(
      @PathVariable String accountNumber) {
    return ResponseEntity.ok(transactionService.getTransactionHistory(accountNumber));

  }

  @PostMapping("/{transactionId}/verify")
  public ResponseEntity<TransactionResponse> verifyOtp(
      @PathVariable UUID transactionId,
      @RequestParam String otp

  ) {
    log.info("OTP verification request - transaction: {}", transactionId);

    return ResponseEntity.ok(transactionService.verifyOTP(transactionId.toString(), otp));

  }

}
