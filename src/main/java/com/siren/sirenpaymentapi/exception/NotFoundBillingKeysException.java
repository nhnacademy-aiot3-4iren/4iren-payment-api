package com.siren.sirenpaymentapi.exception;

//404
public class NotFoundBillingKeysException extends RuntimeException {
    public NotFoundBillingKeysException(Long billingKeyId) {
        super("Billing key=" + billingKeyId + "를 찾을 수 없습니다.");
    }

    public NotFoundBillingKeysException(String message) {
        super(message);
    }
}
