package com.siren.sirenpaymentapi.gateway;

import com.siren.sirenpaymentapi.domain.Provider;
import com.siren.sirenpaymentapi.dto.gateway.ChargeResult;
import com.siren.sirenpaymentapi.dto.gateway.ConfirmedBillingKey;
import com.siren.sirenpaymentapi.dto.gateway.RegistrationStart;

import java.util.Map;

/**
 * Toss/카카오 API를 공통된 모양으로 감싸는 인터페이스.
 * 새 PG가 추가되면 이 인터페이스의 구현체 하나 + Provider enum 값 하나만 추가하면 되고
 * 이걸 호출하는 쪽(BillingKeyRegistrationService, SubscriptionChargeCoordinator 등)은 안 건드려도 됨.
 */
public interface RecurringPaymentGateway {

    /**
     * 이 어댑터가 어떤 PG를 담당하는지. RecurringPaymentGatewayRegistry가 Provider -> 어댑터 매핑을 만들 때 씀.
     */
    Provider getProvider();

    /**
     * 결제수단 등록을 시작할 때 호출 (호출부: 아직 미구현 - 등록 요청 컨트롤러가 담당 예정).
     * 사용자를 PG 인증 화면으로 보낼 리다이렉트 URL을 발급받는다. 이 시점엔 billing_keys row를 만들지 않는다.
     */
    RegistrationStart startRegistration(Long userId, String returnUrl);

    /**
     * PG 인증이 끝나고 콜백이 도착했을 때 호출 (호출부: 아직 미구현 - 콜백 수신 컨트롤러가 담당 예정).
     * 콜백 파라미터로 등록을 확정하고, 이때 처음으로 저장 가능한 providerCredential이 나온다.
     */
    ConfirmedBillingKey confirmRegistration(Map<String, String> callbackParams);

    /**
     * 정기 청구를 시도할 때 호출
     * PaymentsService.prepareCharge로 READY row가 이미 커밋된 뒤에 호출돼야 한다(트랜잭션 밖에서 실행).
     */
    ChargeResult charge(String providerCredential, Long amount, String orderId);

    /**
     * 결제수단을 해지할 때 호출
     */
    void revoke(String providerCredential);
}
