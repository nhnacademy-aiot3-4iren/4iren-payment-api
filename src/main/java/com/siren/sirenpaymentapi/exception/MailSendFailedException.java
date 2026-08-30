package com.siren.sirenpaymentapi.exception;

public class MailSendFailedException extends RuntimeException {
    public MailSendFailedException(String message) {
        super(message);
    }
}
