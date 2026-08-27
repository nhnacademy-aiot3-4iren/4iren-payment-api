package com.siren.sirenpaymentapi.exception;

public class InitialChargeFailedException extends RuntimeException {
    public InitialChargeFailedException(String failureReason) {
        super("가입 직후 첫 청구 실패: " + failureReason);
    }
}
