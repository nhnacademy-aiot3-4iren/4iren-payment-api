package com.siren.sirenpaymentapi.scheduler;

import com.siren.sirenpaymentapi.dto.payments.StuckPayment;
import com.siren.sirenpaymentapi.service.SubscriptionChargeService;
import com.siren.sirenpaymentapi.service.basic_service.PaymentsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * PaymentsService.prepareCharge가 READY로 커밋한 뒤 PG 호출 도중 크래시/미처리 예외로 흐름이
 * 끊기면 payments row가 영영 READY로 남음(정상적인 승인 거절은 ChargeResult.failure로 always
 * FAILED까지 정상 처리되므로 여기 안 걸림). 이 배치는 그렇게 방치된 row를 찾아서 기존
 * SubscriptionChargeService.recordFailure를 그대로 태워 FAILED+PAST_DUE로 정리한다 -
 * 그러면 Dunning 재시도가 자연스럽게 다음날 다시 시도한다. Grafana가 payments.status='READY' AND
 * attempted_at < (임계값)인 row 수를 이 배치 주기보다 짧은 간격으로 감시해서 텔레그램으로 알림을 보낸다
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StuckPaymentRecoveryScheduler {
    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");

    private final PaymentsService paymentsService;
    private final SubscriptionChargeService subscriptionChargeService;

    @Value("${payment.stuck-payment-recovery.threshold-minutes:5}")
    private int thresholdMinutes;

    @Scheduled(cron = "${payment.stuck-payment-recovery.cron:0 */10 * * * *}")
    @SchedulerLock(name = "stuckPaymentRecovery", lockAtMostFor = "PT5M", lockAtLeastFor = "PT10S")
    public void recoverStuckPayments() {
        LocalDateTime cutoff = LocalDateTime.now(ZONE_ID).minusMinutes(thresholdMinutes);
        List<StuckPayment> stuck = paymentsService.findStuckInReady(cutoff);

        if (stuck.isEmpty()) {
            return;
        }
        log.warn("READY 상태로 {}분 넘게 방치된 payments 발견 - {}건, 예외적 실패로 정리 시도", thresholdMinutes, stuck.size());

        for (StuckPayment payment : stuck) {
            try {
                // 이 시도 이후로 같은 구독에 더 최신 시도(성공/실패 불문)가 있었으면, 그 최신 시도가 이미
                // 구독 상태를 정확히 반영한 상태다 - 여기서 markPastDue를 또 호출하면 이미 정상화된 구독을
                // 도로 PAST_DUE로 되돌리는 회귀가 생기므로, 이 stale row는 기록만 정리하고 구독은 안 건드린다.
                if (paymentsService.hasNewerAttempt(payment.subscriptionId(), payment.attemptedAt())) {
                    paymentsService.markFailed(payment.paymentId(),
                            "정합성 배치: 이후 재시도로 이미 해결된 stale 시도, READY 방치분 정리", null);
                } else {
                    subscriptionChargeService.recordFailure(payment.paymentId(), payment.subscriptionId(),
                            "정합성 배치: READY 상태로 " + thresholdMinutes + "분 넘게 방치되어 예외적 실패로 처리됨", null);
                }
            } catch (Exception e) {
                log.error("정합성 배치 처리 중 예외 발생 - paymentId={}", payment.paymentId(), e);
            }
        }
    }
}
