package com.banking.transactionservice.Client;

import java.math.BigDecimal;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "account-service", url = "${account.service.url}")
public interface AccountServiceClinet {

  @PutMapping("/api/v1/account/{accountNumber}/deduct")
  String deductBalance(
      @PathVariable String accountNumber,
      @RequestParam BigDecimal amount);

  @PutMapping("/{accounNumber/credit}")
  String creditBalance(@PathVariable String accountNumber, @RequestParam BigDecimal amount);
}
