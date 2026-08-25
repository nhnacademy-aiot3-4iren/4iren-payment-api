package com.siren.sirenpaymentapi.service;

import com.siren.sirenpaymentapi.domain.Provider;
import com.siren.sirenpaymentapi.dto.gateway.ChargeResult;
import com.siren.sirenpaymentapi.dto.payments.PreparedCharge;
import com.siren.sirenpaymentapi.gateway.RecurringPaymentGatewayRegistry;
import com.siren.sirenpaymentapi.service.basic_service.PaymentsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 스케줄러가 findDueForBilling으로 찾은 구독마다 이 클래스를 호출해서
 * (1) paymentsService.prepareCharge로 READY row 커밋
 * -> (2) PG 어댑터 호출(트랜잭션 밖)
 * -> (3) 결과에 따라 subscriptionChargeService.recordSuccess/recordFailure 호출
 * 이 클래스 자체엔 @Transactional을 붙이지 않는다 - PG 호출을 트랜잭션 밖에 두기 위함.
 * 호출부(스케줄러)는 자신의 트랜잭션 안에서 조회한 구독 엔티티의 스칼라 값만 뽑아서 넘겨야 한다 -
 * detached 엔티티를 그대로 넘기면 billingKey(LAZY) 접근 시 LazyInitializationException이 난다.
 */
@Component
@RequiredArgsConstructor
public class SubscriptionChargeCoordinator {
    private final PaymentsService paymentsService;
    private final SubscriptionChargeService subscriptionChargeService;
    private final BillingKeyRegistrationService billingKeyRegistrationService;
    private final RecurringPaymentGatewayRegistry gatewayRegistry;

    // 배치로 결제 청구
    public void chargeSubscription(Long subscriptionId, Long userId, Provider provider, String providerCredential,
                                    Long amount, boolean wasRecovering) {
        PreparedCharge prepared = paymentsService.prepareCharge(subscriptionId, amount); // 결제 청구 준비

        ChargeResult result = gatewayRegistry.getGateway(provider) // 게이트웨이 찾아오기
                .charge(providerCredential, amount, prepared.orderId()); // 청구 API

        if (result.success()) {
            subscriptionChargeService.recordSuccess(prepared.paymentId(), subscriptionId, wasRecovering,
                    result.providerTransactionId(), result.payToken(), result.rawResponse());
            return;
        }

        if (result.billingKeyRevoked()) {
            // 콜백 유실 대비 방어 로직 - REMOVED 콜백을 못 받았어도 청구 시점에 빌링키 소멸을 확인하면 여기서 처리
            subscriptionChargeService.recordFailureFromRevokedBillingKey(prepared.paymentId(),
                    result.failureReason(), result.rawResponse());
            billingKeyRegistrationService.revokeByProviderNotice(userId);
            return;
        }

        subscriptionChargeService.recordFailure(prepared.paymentId(), subscriptionId,
                result.failureReason(), result.rawResponse());
    }
}
