package com.banking.transactionservice.Entity;
/* 
 * Transaction flow
 * PENDING => PROCESSING => COMPLETED
 *                      -> PENDING_VERIFICATION  (suspecious detected)
 *                      -> COMPLETED(verified)
 *                      -> FLAGGED (Refund)
 *                  -> FAILED
 * */

public enum TransactionStatus {
  PENDING,
  PROCESSING,
  PENDING_VERIFICATION,
  COMPLETED,
  FAILED,
  FLAGGED

}
