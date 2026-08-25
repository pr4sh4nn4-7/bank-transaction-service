package com.banking.transactionservice.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.banking.transactionservice.DTO.TransactionResponse;
import com.banking.transactionservice.Entity.Transaction;

@Repository
public interface TransactionRepo extends JpaRepository<Transaction, String> {

  Optional<Transaction> findById(String transactionId);

  Optional<List<Transaction>> findBySenderAccountNumberOrderByCreatedAtDesc(String accountNumber);

}
