package com.siren.sirenpaymentapi.exception;

public class AccountApiUnavailableException extends RuntimeException {
    public AccountApiUnavailableException(Long userId) {
        super("userId: " + userId+"의 이메일을 받아올 수 없음");
    }
}
