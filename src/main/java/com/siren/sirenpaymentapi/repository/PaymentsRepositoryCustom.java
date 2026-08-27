package com.siren.sirenpaymentapi.repository;

import com.siren.sirenpaymentapi.dto.payments.PaymentHistoryResponse;
import com.siren.sirenpaymentapi.dto.payments.StuckPayment;

import java.time.LocalDateTime;
import java.util.List;

public interface PaymentsRepositoryCustom {

    // 크래시/타임아웃으로 READY 상태에 오래 멈춰있는 row 찾기 (정합성 배치용, StuckPaymentRecoveryScheduler가 호출)
    List<StuckPayment> findStuckInReady(LocalDateTime cutoff);

    // subscriptionId에 after 이후로 시도된 다른 payment가 있는지. 있으면 그 최신 시도가 이미 구독 상태를
    // 정확히 반영했다는 뜻이라 stuck row는 구독 상태를 건드리지 않고 기록만 정리해야 한다
    boolean hasNewerAttempt(Long subscriptionId, LocalDateTime after);

    // 마이페이지 결제내역 조회용 - 최신순
    List<PaymentHistoryResponse> findByUserId(Long userId);
}
