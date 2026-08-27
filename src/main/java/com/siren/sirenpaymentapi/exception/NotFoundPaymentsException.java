package com.siren.sirenpaymentapi.exception;

public class NotFoundPaymentsException extends RuntimeException {
    public NotFoundPaymentsException(Long paymentsId) {
        super("Payments Id=" + paymentsId + "를 찾을 수 없습니다.");
    }
}
