package com.siren.sirenpaymentapi.scheduler;

import com.siren.sirenpaymentapi.service.basic_service.SubscriptionsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 사용자가 직접 해지(CANCELED)한 구독은 currentPeriodEnd까지는 계속 OWNER를 유지해야 해서
 * 강등 이벤트를 해지 시점에 바로 안 쏜다
 * 이 스케줄러가 매일 돌면서 계약기간이 실제로 끝난 CANCELED 구독을 찾아 그제서야
 * downgradeAfterCancelation()을 태운다(status는 CANCELED로 유지, Account로 NORMAL 강등 이벤트만 발행
 *  Dunning 소진으로 인한 EXPIRED와는 사유가 달라서 status를 안 바꿈).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CanceledSubscriptionExpiryScheduler {
    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");

    private final SubscriptionsService subscriptionsService;

    @Scheduled(cron = "${payment.canceled-expiry.cron:0 30 1 * * *}")
    @SchedulerLock(name = "canceledSubscriptionExpiry", lockAtMostFor = "PT30M", lockAtLeastFor = "PT1M")
    public void expireCanceledSubscriptions() {
        List<Long> subscriptionIds = subscriptionsService.findCanceledPastPeriodEnd(LocalDateTime.now(ZONE_ID));
        if (subscriptionIds.isEmpty()) {
            return;
        }
        log.info("해지 후 계약기간이 끝난 구독 정리 시작 - 대상 {}건", subscriptionIds.size());

        for (Long subscriptionId : subscriptionIds) {
            try {
                subscriptionsService.downgradeAfterCancelation(subscriptionId);
            } catch (Exception e) {
                log.error("해지 구독 만료 처리 중 예외 발생 - subscriptionId={}", subscriptionId, e);
            }
        }
    }
}
