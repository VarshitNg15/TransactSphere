package com.transactsphere.transaction.exception;

public class FraudulentTransactionException extends RuntimeException {
    public FraudulentTransactionException(String message) {
        super(message);
    }
}
