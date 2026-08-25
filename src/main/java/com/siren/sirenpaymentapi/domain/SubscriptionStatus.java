package com.siren.sirenpaymentapi.domain;

public enum SubscriptionStatus {
    ACTIVE, // 구독 활성화
    PAST_DUE, // 결제 실패했는 데 아직 유예기간 중
    EXPIRED, // 구독 만료
    CANCELED // 구독 취소
}
