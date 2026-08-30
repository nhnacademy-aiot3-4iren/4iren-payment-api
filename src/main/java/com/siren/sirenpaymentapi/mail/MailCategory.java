package com.siren.sirenpaymentapi.mail;

import lombok.Getter;

@Getter
public enum MailCategory {
    PAY_SUCCESS("결제가 성공적으로 완료되었습니다."),
    PAST_DUE("결제실패 하였습니다."),
    EXPIRED("결제가 만료되었습니다."),
    CANCELED("구독이 해지접수 완료되었습니다."),
    ENDED("구독이 중지됐습니다.");

    private final String subject;

    MailCategory(String subject) {
        this.subject = subject;
    }
}
