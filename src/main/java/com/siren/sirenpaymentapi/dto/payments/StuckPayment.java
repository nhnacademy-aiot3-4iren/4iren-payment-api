package com.siren.sirenpaymentapi.dto.payments;

import java.time.LocalDateTime;

/**
 * READY 상태로 장시간 방치된(크래시/예외로 청구 흐름이 중간에 끊긴) payments row - 정합성 배치용.
 * subscriptionId만 담는 이유: StuckPaymentRecoveryScheduler가 트랜잭션 경계를 넘나드는 배치라
 * detached 엔티티를 넘기면 LazyInitializationException 위험이 있어서(findDueForBilling과 동일 원칙).
 * attemptedAt은 "이후 같은 구독에 더 최신 시도가 있었는지" 판단용 - 있으면 그 최신 시도가 이미 구독
 * 상태를 정확히 반영했으므로 이 stale row는 구독 상태를 건드리지 않고 기록만 정리해야 한다.
 */
public record StuckPayment(Long paymentId, Long subscriptionId, LocalDateTime attemptedAt) {
}
