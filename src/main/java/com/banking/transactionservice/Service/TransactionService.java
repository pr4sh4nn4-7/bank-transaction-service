package com.banking.transactionservice.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.banking.transactionservice.Client.AccountServiceClinet;
import com.banking.transactionservice.DTO.TransactionResponse;
import com.banking.transactionservice.DTO.TransferRequest;
import com.banking.transactionservice.Entity.Transaction;
import com.banking.transactionservice.Entity.TransactionStatus;
import com.banking.transactionservice.Entity.TransactionType;
import com.banking.transactionservice.Event.TransactionCompletedEvent;
import com.banking.transactionservice.Event.TransactionInitiatedEvent;
import com.banking.transactionservice.Exception.AccountNumberNotFoundException;
import com.banking.transactionservice.Exception.TransactionIdNotFoundException;
import com.banking.transactionservice.Repository.TransactionRepo;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {
  private final TransactionRepo transactionRepo;
  private static final String TRANSACTION_INITIATED_TOPIC = "transaction.initiated";
  private static final String TRANSACTION_COMPLETED = "transaction.completed";
  private static final String TRANSACTION_REFUNDED = "transaction.refunded";
  private static final String TRNSACTION_FRAUD_DETECTED_TOPIC = "fraud.detected";
  private final AccountServiceClinet accountServiceClinet;
  private final KafkaTemplate<String, Object> kafkaTemplate;
  private final RedisTemplate<String, String> redisTemplate;

  /*
   * 1. initialize transfer
   * 2. deducts from sender via feign
   * 3. save the transaction as processing
   * 4. send the event to detect fraud
   * 4. returns
   *
   *
   */

  public TransactionResponse transfer(TransferRequest request) {
    log.info("Start- Transfer : {} -> {} amount {}", request.getSenderAccountNumber(),
        request.getReceiverAccountNumber(), request.getAmount());
    // deduct form sender

    accountServiceClinet.deductBalance(request.getSenderAccountNumber(), request.getAmount());
    Transaction transaction = new Transaction();
    transaction.setSenderAccountNumber(request.getSenderAccountNumber());
    transaction.setReceiverAccountNumber(request.getReceiverAccountNumber());
    transaction.setAmount(request.getAmount());
    transaction.setType(TransactionType.TRANSFER);
    transaction.setStatus(TransactionStatus.PROCESSING);
    transaction.setDescription(request.getDescription());
    transaction.setReferenceNumber(UUID.randomUUID().toString());
    Transaction savedTransaction = transactionRepo.save(transaction);
    log.info("Transaction saved as PROCESSING {}", savedTransaction.getId());
    TransactionInitiatedEvent event = new TransactionInitiatedEvent(
        savedTransaction.getId(),
        savedTransaction.getSenderAccountNumber(),
        savedTransaction.getReceiverAccountNumber(),
        savedTransaction.getAmount(),
        savedTransaction.getDescription());
    kafkaTemplate.send(TRANSACTION_INITIATED_TOPIC, savedTransaction.getId(), event);
    log.info("SAGA STEP 2 - TRANSACTION INITIATED EVENET published: {}", savedTransaction.getId());
    return mapToTransactionResponse(savedTransaction);

  }

  public TransactionResponse getTransaction(String transactionId) {
    return mapToTransactionResponse(transactionRepo.findById(transactionId)
        .orElseThrow(() -> new TransactionIdNotFoundException("Transaction not" + transactionId + "valid")));

  }

  public TransactionResponse verifyOTP(String transactionId, String otp) {

    log.info("Otp verification for the transaction : {}", transactionId);
    Transaction transaction = transactionRepo.findById(transactionId)
        .orElseThrow(() -> new TransactionIdNotFoundException("Transaction Id not found"));

    String otpKey = "verification:otp" + transactionId;
    String storedOtp = redisTemplate.opsForValue().get(otpKey);
    if (storedOtp == null) {
      log.warn("otp expired for transaction: {}", transactionId);
      compensateTransaction(transaction, "Otp expired - transaction cancelled");
      return mapToTransactionResponse(transaction);
    }
    if (!storedOtp.equals(otp)) {
      log.warn("Wrong Otp - blocking account and refunding : {}", transactionId);
      redisTemplate.delete(otpKey);
      blockAccountAndCompensate(transaction, "Wrong Otp entered transaction cancelled");
      return mapToTransactionResponse(transaction);

    }
    // otp correct - complete transaction

    log.info("otp verified - completing transaction: {}", transactionId);
    redisTemplate.delete(otpKey);
    completeTransaction(transaction);
    return mapToTransactionResponse(transaction);

  }

  private void compensateTransaction(Transaction transaction, String reason) {
    log.warn("SAGA COMPENSATION - refunding : {} amount {}", transaction.getSenderAccountNumber(),
        transaction.getAmount());
    // CREDIT MONEY BACK TO SENDER SYNCHRONOUSLY
    accountServiceClinet.creditBalance(transaction.getSenderAccountNumber(), transaction.getAmount());
    transaction.setStatus(TransactionStatus.FLAGGED);
    transaction.setFailureReason(reason + " - SAGA compensation executed , amount redunded at " + LocalDateTime.now());
    transactionRepo.save(transaction);
    // publish refund evend notification service will alert user

    Map<String, Object> refundEvent = new HashMap<>();
    refundEvent.put("transactionId", transaction.getId());
    refundEvent.put("senderAccountNumber", transaction.getSenderAccountNumber());
    refundEvent.put("amount", transaction.getAmount());
    refundEvent.put("reason", reason);

    kafkaTemplate.send(TRANSACTION_REFUNDED, transaction.getId(), refundEvent);

    log.info("SAGA COMPENSATION complete - {} refund to {}", transaction.getAmount(),
        transaction.getSenderAccountNumber());

  }

  private void blockAccountAndCompensate(Transaction transaction, String reason) {
    // publis fraud.detected => account service will block account
    Map<String, Object> fraudEvent = new HashMap<>();
    fraudEvent.put("transactionId", transaction.getId());
    fraudEvent.put("accountNumber", transaction.getSenderAccountNumber());
    fraudEvent.put("reason", reason);
    kafkaTemplate.send(TRNSACTION_FRAUD_DETECTED_TOPIC, transaction.getSenderAccountNumber(), fraudEvent);
    log.warn("fraud.detected published - account: {} will be blocked. Kindly contact to the bank",
        transaction.getSenderAccountNumber());

    // COMPENSATION - REFUND SENDER
    compensateTransaction(transaction, reason);
  }

  private void completeTransaction(Transaction transaction) {
    transaction.setStatus(TransactionStatus.COMPLETED);
    transaction.setCompletedAt(LocalDateTime.now());
    transactionRepo.save(transaction);

    TransactionCompletedEvent completedEvent = new TransactionCompletedEvent(
        transaction.getId(),
        transaction.getSenderAccountNumber(),
        transaction.getReceiverAccountNumber(),
        transaction.getAmount(),
        transaction.getDescription());

    kafkaTemplate.send(TRANSACTION_COMPLETED, transaction.getId(), completedEvent);
    log.info("SAGA COMPLETE - transaction : {} completed", transaction.getId());
  }

  public List<TransactionResponse> getTransactionHistory(String accountNumber) {
    List<Transaction> transaction = transactionRepo.findBySenderAccountNumberOrderByCreatedAtDesc(accountNumber)
        .orElseThrow(() -> new AccountNumberNotFoundException("Account " + accountNumber + "Not found"));
    return transaction.stream().map(this::mapToTransactionResponse).collect(Collectors.toList());

  }

  public void processCleanResult(String transactionId) {

    Transaction transaction = transactionRepo.findById(transactionId)
        .orElseThrow(() -> new TransactionIdNotFoundException("Transaction Id not found"));
    if (transaction.getStatus() != TransactionStatus.PROCESSING) {
      log.warn("Transaction {} not PROCESSING - skipping", transactionId);
      return;
    }
    completeTransaction(transaction);

  }

  private TransactionResponse mapToTransactionResponse(Transaction transaction) {
    TransactionResponse response = new TransactionResponse();
    response.setId(transaction.getId());
    response.setSenderAccountNumber(transaction.getSenderAccountNumber());
    response.setReceiverAccountNumber(transaction.getReceiverAccountNumber());
    response.setDescription(transaction.getDescription());
    response.setAmount(transaction.getAmount());
    response.setStatus(transaction.getStatus());
    response.setType(transaction.getType());
    response.setFailureReason(transaction.getFailureReason());
    response.setReferenceNumber(transaction.getReferenceNumber());
    response.setCreatedAt(transaction.getCreatedAt());
    response.setCompletedAt(transaction.getCompletedAt());
    return response;

  }

}
