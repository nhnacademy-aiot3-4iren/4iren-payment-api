package com.siren.sirenpaymentapi.exception;

public class InactiveBillingKeyException extends RuntimeException {
    public InactiveBillingKeyException(String message) {
        super(message);
    }
}
