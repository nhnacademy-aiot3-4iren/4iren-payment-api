package com.siren.sirenpaymentapi.exception;

public class NotFoundSubscriptionException extends RuntimeException {
    public NotFoundSubscriptionException(Long subscriptionId) {
        super("Subscription ID=" + subscriptionId + "를 찾을 수 없습니다.");
    }

    public NotFoundSubscriptionException(String message) {
        super(message);
    }
}
