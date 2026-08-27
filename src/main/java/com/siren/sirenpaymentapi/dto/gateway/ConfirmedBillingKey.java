package com.siren.sirenpaymentapi.dto.gateway;

/**
 * 등록이 확정됐을 때(RecurringPaymentGateway.confirmRegistration) 반환되는 값.
 * providerCredential은 BillingKeys.providerCredential에 그대로 저장할 JSON 문자열이다.
 * PG별로 내부 구조가 다르고(Toss billingKey / 카카오 sid / 네이버 recurrentId), 이 값을 다시 꺼내 쓰는 것도
 * 해당 PG의 어댑터만 알면 된다.
 */
public record ConfirmedBillingKey(String providerCredential, String maskedInfo) {
}
