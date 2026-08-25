package com.siren.sirenpaymentapi.service.basic_service;

import com.siren.sirenpaymentapi.domain.PaymentStatus;
import com.siren.sirenpaymentapi.domain.entity.Payments;
import com.siren.sirenpaymentapi.domain.entity.Subscriptions;
import com.siren.sirenpaymentapi.dto.payments.PreparedCharge;
import com.siren.sirenpaymentapi.dto.payments.StuckPayment;
import com.siren.sirenpaymentapi.exception.NotFoundPaymentsException;
import com.siren.sirenpaymentapi.repository.PaymentsRepository;
import com.siren.sirenpaymentapi.repository.SubscriptionsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentsService {
    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");
    private final PaymentsRepository paymentsRepository;
    private final SubscriptionsRepository subscriptionsRepository;

    /**
     * 정기 청구를 시도하기 직전에 호출
     * order_id를 발급하고 READY 상태로 커밋해두는 게 핵심 -> PG 호출은 이 메서드가 리턴(커밋)된 이후에 해야 한다.
     * 그래야 PG 호출 도중 서버가 죽어도 시도했다는 기록이 남아 크래시 복구가 가능하다.
     * subscriptionId만 받는 이유: 호출부가 트랜잭션 경계를 넘나드는 코디네이터라 detached 엔티티를 넘길 수 없음
     * getReferenceById로 추가 SELECT 없이 FK만 참조.
     */
    public PreparedCharge prepareCharge(Long subscriptionId, Long amount) {
        Subscriptions subscriptions = subscriptionsRepository.getReferenceById(subscriptionId);
        String orderId = UUID.randomUUID().toString();
        Payments payments = Payments.builder()
                .subscription(subscriptions)
                .orderId(orderId)
                .amount(amount)
                .status(PaymentStatus.READY)
                .attemptedAt(LocalDateTime.now(ZONE_ID))
                .build();
        paymentsRepository.save(payments);
        return new PreparedCharge(payments.getId(), orderId);
    }

    /**
     * PG 승인 API가 성공 응답을 준 직후 호출. SubscriptionChargeService.recordSuccess에서
     * 구독 갱신(advanceBillingCycle/recoverActive)과 같은 트랜잭션으로 묶여서 실행됨.
     */
    @Transactional
    public void markSucceeded(Long paymentsId, String providerTransactionId, String payToken, String rawResponse) {
        findById(paymentsId).markSucceeded(providerTransactionId, payToken, rawResponse);
    }

    /**
     * PG 승인 API가 실패했거나 호출 중 예외가 났을 때 호출. SubscriptionChargeService.recordFailure에서
     * 구독 PAST_DUE 전이와 같은 트랜잭션으로 묶여서 실행됨.
     */
    @Transactional
    public void markFailed(Long paymentsId, String failureReason, String rawResponse) {
        findById(paymentsId).markFailed(failureReason, rawResponse);
    }

    private Payments findById(Long paymentsId) {
        return paymentsRepository.findById(paymentsId)
                .orElseThrow(()->new NotFoundPaymentsException(paymentsId));
    }

    /**
     * 정합성 배치(StuckPaymentRecoveryScheduler)가 호출 - cutoff보다 오래 전에 READY로 커밋된 채
     * 안 바뀐 row를 찾는다(크래시/미처리 예외로 청구 흐름이 중간에 끊긴 경우).
     */
    public List<StuckPayment> findStuckInReady(LocalDateTime cutoff) {
        return paymentsRepository.findStuckInReady(cutoff);
    }

    /**
     * 정합성 배치가 stuck row를 처리하기 전에 확인 - 이 시도 이후로 같은 구독에 더 최신 시도가 있었다면
     * 그 최신 시도가 이미 구독 상태를 정확히 반영했으므로, stuck row는 구독을 건드리면 안 된다
     * (건드리면 최신 성공을 도로 PAST_DUE로 되돌리는 회귀 버그가 생김).
     */
    public boolean hasNewerAttempt(Long subscriptionId, LocalDateTime after) {
        return paymentsRepository.hasNewerAttempt(subscriptionId, after);
    }
}
