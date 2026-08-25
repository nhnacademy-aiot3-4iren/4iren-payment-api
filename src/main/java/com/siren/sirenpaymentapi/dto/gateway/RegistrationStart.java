package com.siren.sirenpaymentapi.dto.gateway;

/**
 * 등록 시작 시(RecurringPaymentGateway.startRegistration) 반환되는 값.
 * @param redirectUrl 여기로 사용자를 보내면 PG 인증화면으로 이동
 * @param correlationKey Toss: billingKey , Kakao: orderId - 브라우저 리다이렉트로 되돌아오는 값
 * @param providerReference  toss는 빌링키 쓰고(그래서 null) 카카오는 tid -> approve 호출에 필수
 */
public record RegistrationStart(String redirectUrl, String correlationKey, String providerReference) {
}
