package com.banking.transactionservice.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.KafkaListeners;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import com.banking.transactionservice.Entity.Transaction;
import com.banking.transactionservice.Entity.TransactionStatus;
import com.banking.transactionservice.Exception.TransactionIdNotFoundException;
import com.banking.transactionservice.Repository.TransactionRepo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionEventConsumer {
  /*
   * consume verification required
   * generate otp asnd ask user to verify
   *
   *
   */

  private final RedisTemplate<String, String> redisTemplate;
  private static final Long OTP_EXPIRY_MINUTES = 5L;
  private final KafkaTemplate<String, Object> kafkaTemplate;

  private final TransactionRepo transactionRepo;
  private final static String TRANSACTION_OTP_GENERATED_TOPIC = "transaction.otp.generated";
  private final TransactionService transactionService;

  @KafkaListener(topics = "verification.required")
  public void consumeVerificationRequired(
      @Payload Map<String, Object> payload) {
    try {
      String transactionId = payload.get("transactionId").toString();
      String accountNumber = payload.get("accountNumber").toString();
      String reason = payload.get("reason").toString();
      log.info("verification required - transaction : {} reqason: {} ", transactionId, reason);

      Transaction transaction = transactionRepo.findById(transactionId)
          .orElseThrow(() -> new TransactionIdNotFoundException("Transaction data not found " + transactionId));

      if (transaction.getStatus() != TransactionStatus.PROCESSING) {
        log.warn("Transaction {} not PROCESSING - skipping", transactionId);
        return;
      }
      // generate 6 digit otp
      String otp = String.format("%06d", (int) Math.random() * 900000 + 100000);
      // store otp in redis - expire in 5 minutes
      String otpkey = "verification:otp" + transactionId;
      redisTemplate.opsForValue().set(otpkey, otp, OTP_EXPIRY_MINUTES, TimeUnit.MINUTES);
      // update status

      transaction.setStatus(TransactionStatus.PENDING_VERIFICATION);
      transactionRepo.save(transaction);

      log.info("Otp generates for transaction: {} expires in {} min", transactionId, OTP_EXPIRY_MINUTES);
      // Notify user
      Map<String, Object> otpevent = new HashMap<>();
      otpevent.put("transactionId", transactionId);
      otpevent.put("accountNumber", accountNumber);
      otpevent.put("reason", reason);
      otpevent.put("otp", otp);
      otpevent.put("amount", payload.get("amount"));

      kafkaTemplate.send(TRANSACTION_OTP_GENERATED_TOPIC, transactionId, otpevent);

    } catch (Exception e) {
      log.error("Error handling verification required : {}", e.getMessage());
    }
  }

  @KafkaListener(topics = "fraud.check.clean")
  public void consumeFraudCheckCleanResult(
      @Payload Map<String, Object> payload) {

    try {
      String transactionId = payload.get("transactionId").toString();
      String accoutNumber = payload.get("accountNumber").toString();

      transactionService.processCleanResult(transactionId);

    } catch (Exception e) {

      log.error("Error processing fraud check result: {}", e.getMessage());
    }

  }

}
