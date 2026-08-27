package com.siren.sirenpaymentapi.scheduler;

import com.siren.sirenpaymentapi.dto.subscriptions.BillingTarget;
import com.siren.sirenpaymentapi.service.SubscriptionChargeCoordinator;
import com.siren.sirenpaymentapi.service.basic_service.SubscriptionsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;

/**
 * 매일 청구일 도래한 구독(ACTIVE 정상 갱신 + PAST_DUE 재시도)을 찾아 자동 청구를 시도한다.
 * 다중 인스턴스에서 동시 실행돼도 shedlock이 한 인스턴스만 실행하게 막아준다(이중 청구 방지).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionBillingScheduler {
    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");

    private final SubscriptionsService subscriptionsService;
    private final SubscriptionChargeCoordinator subscriptionChargeCoordinator;

    @Value("${payment.billing.max-concurrent-charges:10}")
    private int maxConcurrentCharges;

    @Scheduled(cron = "${payment.billing.cron:0 0 1 * * *}")
    @SchedulerLock(name = "subscriptionBilling", lockAtMostFor = "PT30M", lockAtLeastFor = "PT1M")
    public void runDailyBilling() {
        List<BillingTarget> targets = subscriptionsService.findDueForBilling(LocalDate.now(ZONE_ID));
        log.info("자동청구 스케줄러 시작 - 대상 {}건", targets.size());

        Semaphore semaphore = new Semaphore(maxConcurrentCharges);
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> futures = targets.stream()
                    .<Future<?>>map(target -> executor.submit(() -> chargeOne(target, semaphore)))
                    .toList();

            for (Future<?> future : futures) {
                future.get();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("자동청구 스케줄러가 대기 중 인터럽트됨", e);
        } catch (Exception e) {
            log.error("자동청구 스케줄러 실행 중 예외 발생", e);
        }

        log.info("자동청구 스케줄러 종료");
    }

    private void chargeOne(BillingTarget target, Semaphore semaphore) {
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        try {
            subscriptionChargeCoordinator.chargeSubscription(target.subscriptionId(), target.userId(),
                    target.provider(), target.providerCredential(), target.amount(), target.wasRecovering());
        } catch (Exception e) {
            // 이 구독 하나만 실패로 남기고 배치 전체는 계속 진행되게 하는 최소 방어.
            // 여기서 예외가 터지면 해당 행은 READY로 남고 다른 건 수행함
            log.error("구독 자동청구 처리 중 예외 발생 - subscriptionId={}", target.subscriptionId(), e);
        } finally {
            semaphore.release();
        }
    }
}
