package com.siren.sirenpaymentapi.gateway.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.siren.sirenpaymentapi.adaptor.KakaoAdaptor;
import com.siren.sirenpaymentapi.domain.Provider;
import com.siren.sirenpaymentapi.dto.gateway.ChargeResult;
import com.siren.sirenpaymentapi.dto.gateway.ConfirmedBillingKey;
import com.siren.sirenpaymentapi.dto.gateway.RegistrationStart;
import com.siren.sirenpaymentapi.dto.kakao.response.ApproveResponse;
import com.siren.sirenpaymentapi.dto.kakao.response.InactiveResponse;
import com.siren.sirenpaymentapi.dto.kakao.response.KakaoErrorResponse;
import com.siren.sirenpaymentapi.dto.kakao.response.ReadyResponse;
import com.siren.sirenpaymentapi.dto.kakao.response.SubscriptionResponse;
import com.siren.sirenpaymentapi.exception.BillingKeyRegistrationException;
import com.siren.sirenpaymentapi.exception.BillingKeyRemoveException;
import com.siren.sirenpaymentapi.exception.JsonConversionException;
import com.siren.sirenpaymentapi.gateway.RecurringPaymentGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;
import java.util.UUID;

/**
 * 카카오페이 정기결제 어댑터
 */
@Component
@RequiredArgsConstructor
public class KakaoPaymentGateway implements RecurringPaymentGateway {
    private static final int ERROR_SID_DEACTIVATED = -751;

    private final KakaoAdaptor kakaoAdaptor;
    private final ObjectMapper objectMapper;

    @Override
    public Provider getProvider() {
        return Provider.KAKAO_PAY;
    }

    // 정기결제 SID 발급 1단계(ready) - orderId는 여기서 직접 생성, correlationKey로 리턴해서 컨트롤러가 캐시 키로 씀
    @Override
    public RegistrationStart startRegistration(Long userId, String returnUrl) {
        String orderId = UUID.randomUUID().toString();
        // 콜백이 브라우저 GET 리다이렉트라 pg_token만 돌아옴 - orderId를 우리가 직접 URL에 심어둬야
        // 콜백 수신 시 어떤 등록 시도였는지 알 수 있음
        String approvalUrl = returnUrl + "?orderId=" + orderId;
        ReadyResponse response = kakaoAdaptor.ready(userId, orderId, approvalUrl);

        if (response == null) {
            throw new BillingKeyRegistrationException(Provider.KAKAO_PAY);
        }
        return new RegistrationStart(response.nextRedirectPcUrl(), orderId, response.tid());
    }

    @Override
    public ConfirmedBillingKey confirmRegistration(Map<String, String> callbackParams) {
        String tid = callbackParams.get("tid");
        String orderId = callbackParams.get("orderId");
        Long userId = Long.valueOf(callbackParams.get("userId"));
        String pgToken = callbackParams.get("pg_token");

        ApproveResponse response = kakaoAdaptor.approve(tid, orderId, userId, pgToken);

        if (response == null || response.sid() == null) {
            throw new BillingKeyRegistrationException(Provider.KAKAO_PAY);
        }
        return new ConfirmedBillingKey(writeJson(new KakaoCredential(response.sid(), userId)),
                response.paymentMethodType());
    }

    // 2회차 이후 정기결제 승인
    @Override
    public ChargeResult charge(String providerCredential, Long amount, String orderId) {
        KakaoCredential credential = readCredential(providerCredential);
        try {
            SubscriptionResponse response = kakaoAdaptor.charge(credential.sid(), amount, orderId, credential.userId());
            if (response == null) {
                return ChargeResult.failure("응답 없음", null);
            }
            // payToken은 Toss 전용 개념(환불/상태조회 토큰) - 카카오는 sid/tid만 있어서 null
            return ChargeResult.success(response.tid(), null, writeJson(response));
        } catch (RestClientResponseException e) {
            String rawResponse = e.getResponseBodyAsString();
            KakaoErrorResponse error = readError(rawResponse);
            if (error != null && Integer.valueOf(ERROR_SID_DEACTIVATED).equals(error.errorCode())) {
                return ChargeResult.billingKeyRevoked(error.errorMessage(), rawResponse);
            }
            return ChargeResult.failure(e.getMessage(), rawResponse);
        }
    }

    // 정기결제 해지
    @Override
    public void revoke(String providerCredential) {
        String sid = readCredential(providerCredential).sid();
        InactiveResponse response = kakaoAdaptor.inactive(sid);

        if (response == null || !"INACTIVE".equals(response.status())) {
            throw new BillingKeyRemoveException(
                    "카카오페이 빌링키 삭제 실패: " + (response == null ? "응답 없음" : response.status()));
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new JsonConversionException("JSON 직렬화 실패", e);
        }
    }

    private KakaoCredential readCredential(String providerCredential) {
        try {
            return objectMapper.readValue(providerCredential, KakaoCredential.class);
        } catch (JsonProcessingException e) {
            throw new JsonConversionException("providerCredential JSON 파싱 실패", e);
        }
    }

    // 실패 응답 바디가 항상 {error_code, error_message} 모양이라는 보장이 없어(문서에 예시가 없었음)
    // 파싱 실패 시 예외를 던지지 않고 null만 리턴 - 호출부가 일반 failure로 폴백함
    private KakaoErrorResponse readError(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(rawResponse, KakaoErrorResponse.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    // userId도 같이 저장 - /payment/subscription 호출 시 partner_user_id가 ready 때와 일치해야 해서 필요
    private record KakaoCredential(String sid, Long userId) {
    }
}
