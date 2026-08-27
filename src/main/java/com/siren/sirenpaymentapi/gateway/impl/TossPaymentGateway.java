package com.siren.sirenpaymentapi.gateway.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.siren.sirenpaymentapi.adaptor.TossAdaptor;
import com.siren.sirenpaymentapi.domain.Provider;
import com.siren.sirenpaymentapi.dto.gateway.ChargeResult;
import com.siren.sirenpaymentapi.dto.gateway.ConfirmedBillingKey;
import com.siren.sirenpaymentapi.dto.gateway.RegistrationStart;
import com.siren.sirenpaymentapi.dto.toss.response.BillingKeyStatusResponse;
import com.siren.sirenpaymentapi.dto.toss.response.ChargeResponse;
import com.siren.sirenpaymentapi.dto.toss.response.CreateBillingKeyResponse;
import com.siren.sirenpaymentapi.dto.toss.response.RemoveBillingKeyResponse;
import com.siren.sirenpaymentapi.exception.BillingKeyCallbackException;
import com.siren.sirenpaymentapi.exception.BillingKeyRegistrationException;
import com.siren.sirenpaymentapi.exception.BillingKeyRemoveException;
import com.siren.sirenpaymentapi.exception.InactiveBillingKeyException;
import com.siren.sirenpaymentapi.gateway.RecurringPaymentGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

/**
 * 토스페이(docs-pay.toss.im, 도메인 pay.toss.im) 정기결제 어댑터.
 * 토스페이먼츠(docs.tosspayments.com, api.tosspayments.com)와는 다른 제품 - 혼동 주의.
 * 흐름: 빌링키 생성(리다이렉트 URL 발급) -> 사용자 인증(토스 앱) -> resultCallback 수신 + 상태조회(ACTIVE 확인)
 *       -> 자동결제 승인(bill) -> 해지.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TossPaymentGateway implements RecurringPaymentGateway {
    // 승인 실패만으로 구독 상태를 판단하지 말고, 이 에러코드일 때만 상태조회로 재확인하라는 토스 권장사항
    private static final String ERROR_BILLING_KEY_NOT_FOUND = "COMMON_BILLING_KEY_NOT_FOUND";
    private static final String NO_RESPONSE = "응답 없음";

    private final TossAdaptor tossAdaptor;
    private final ObjectMapper objectMapper;

    @Override
    public Provider getProvider() {
        return Provider.TOSS_PAY;
    }

    /**
     * 빌링키 생성  완료 후 빌링키 상태: CREATED (생성됨)
     * @param userId 결제 당사자 userId
     * @param returnUrl 사용자가 인증을 완료하면 결과를 받을 가맹점 서버 URL 보안상 HTTPS 권장
     * @return
     * String redirectUrl 사용자를 토스 앱으로 보낼 url
     * String correlationKey 아직 인증 안된 빌링 키
     */
    @Override
    public RegistrationStart startRegistration(Long userId, String returnUrl) {
        CreateBillingKeyResponse response = tossAdaptor.createBillingKey(userId, returnUrl);

        if (response == null || response.code() != 0) {
            throw new BillingKeyRegistrationException(Provider.TOSS_PAY,
                    response == null ? NO_RESPONSE : response.errorCode() + " - " + response.msg());
        }
        return new RegistrationStart(response.checkoutUri(), response.billingKey(), null);
    }

    /**
     * 활성화 콜백 받기
     * returnURl로 토스가 POST 요청을 보냄
     * {
     *   "action": "ACTIVATED",
     *   "userId": "tutorial-user-001",
     *   "billingKey": "example-billingKey",
     *   "payMethod": "CARD"
     *  }
     *  등록이면 카드정보, 토스머니면 계좌 정보
     */
    @Override
    public ConfirmedBillingKey confirmRegistration(Map<String, String> callbackParams) {
        String action = callbackParams.get("action");
        if (!"ACTIVATED".equals(action)) {
            throw new BillingKeyCallbackException(Provider.TOSS_PAY, action);
            // action이 REMOVED 이면 토스 쪽 사정으로 빌링키가 삭제된거임 이 경우 구독을 중지하고 재등록을 안내해야함
            // 위 예외가 터지면 어떻게 할 지 고려해보기
        }
        String userId = callbackParams.get("userId");
        // 여기까지 오면 빌링키 활성화 됨. userId의 구독 상태를 활성으로 바꾸면 되지만 아래에서 한 번 더 확인

        BillingKeyStatusResponse status = tossAdaptor.getBillingKeyStatus(userId);

        if (status == null || status.code() != 0 || !"ACTIVE".equals(status.status())) {
            throw new InactiveBillingKeyException("토스페이 빌링키 상태조회 결과가 ACTIVE가 아님: "
                    + (status == null ? NO_RESPONSE : status.status()));
        }

        return new ConfirmedBillingKey(
                writeJson(new TossCredential(status.billingKey(), Long.valueOf(status.userId()))),
                status.payMethod());
    }

    // 자동 결제 승인하기
    @Override
    public ChargeResult charge(String providerCredential, Long amount, String orderId) {
        TossCredential credential = readCredential(providerCredential);
        try {
            ChargeResponse response = tossAdaptor.executeBilling(credential.billingKey(), amount, orderId);
            if (response == null) {
                return ChargeResult.failure(NO_RESPONSE, null);
            }
            String rawResponse = writeJson(response);
            if (response.code() == 0) {
                return ChargeResult.success(response.transactionId(), response.payToken(), rawResponse);
            }
            if (ERROR_BILLING_KEY_NOT_FOUND.equals(response.errorCode()) && isBillingKeyRevoked(credential.userId())) {
                return ChargeResult.billingKeyRevoked(response.msg(), rawResponse);
            }
            return ChargeResult.failure(response.msg(), rawResponse);
        } catch (RestClientResponseException e) {
            return ChargeResult.failure(e.getMessage(), e.getResponseBodyAsString());
        }
    }

    private boolean isBillingKeyRevoked(Long userId) {
        BillingKeyStatusResponse status = tossAdaptor.getBillingKeyStatus(String.valueOf(userId));
        return status != null && status.code() == 0
                && ("REMOVE".equals(status.status()) || "CANCEL".equals(status.status()));
    }

    // 빌링 키 삭제하기
    @Override
    public void revoke(String providerCredential) {
        String billingKey = readCredential(providerCredential).billingKey();
        RemoveBillingKeyResponse response = tossAdaptor.removeBillingKey(billingKey);
        if (response == null || response.code() != 0) {
            throw new BillingKeyRemoveException("토스페이 빌링키 삭제 실패: " + (response == null ? NO_RESPONSE : response.msg()));
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON 직렬화 실패", e);
        }
    }

    private TossCredential readCredential(String providerCredential) {
        try {
            return objectMapper.readValue(providerCredential, TossCredential.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("providerCredential JSON 파싱 실패", e);
        }
    }

    // userId는 상태조회 API가 billingKey가 아니라 userId로 조회해서 같이 들고 있음(charge 실패 시 재확인용)
    private record TossCredential(String billingKey, Long userId) {
    }

}
