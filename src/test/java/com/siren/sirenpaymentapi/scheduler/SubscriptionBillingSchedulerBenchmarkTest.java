package com.siren.sirenpaymentapi.scheduler;

import com.siren.sirenpaymentapi.domain.Provider;
import com.siren.sirenpaymentapi.dto.subscriptions.BillingTarget;
import com.siren.sirenpaymentapi.service.SubscriptionChargeCoordinator;
import com.siren.sirenpaymentapi.service.basic_service.SubscriptionsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;

/**
 * 자동청구 스케줄러의 가상 스레드 병렬화 효과를 실제 runDailyBilling() 호출로 측정한다.
 * PG 호출은 실제 Toss 대신 지연만 흉내낸 mock으로 대체.
 * mvn test -Dtest=SubscriptionBillingSchedulerBenchmarkTest -Dbench.count=50 -Dbench.latency=200
 */
@ExtendWith(MockitoExtension.class)
class SubscriptionBillingSchedulerBenchmarkTest {

    private static final int TARGET_COUNT = Integer.getInteger("bench.count", 100);
    private static final int PG_LATENCY_MS = Integer.getInteger("bench.latency", 20);
    private static final int MAX_CONCURRENT_CHARGES = 10; // 운영 기본값(payment.billing.max-concurrent-charges)과 동일

    @Mock
    private SubscriptionsService subscriptionsService;

    @Mock
    private SubscriptionChargeCoordinator subscriptionChargeCoordinator;

    @InjectMocks
    private SubscriptionBillingScheduler subscriptionBillingScheduler;

    private List<BillingTarget> targets;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(subscriptionBillingScheduler, "maxConcurrentCharges", MAX_CONCURRENT_CHARGES);

        targets = IntStream.rangeClosed(1, TARGET_COUNT)
                .mapToObj(i -> new BillingTarget((long) i, (long) i, Provider.TOSS_PAY, "credential" + i, 29000L, false))
                .toList();

        lenient().when(subscriptionsService.findDueForBilling(any(LocalDate.class))).thenReturn(targets);

        // PG 호출 지연만 흉내낸다
        lenient().doAnswer(invocation -> {
            Thread.sleep(PG_LATENCY_MS);
            return null;
        }).when(subscriptionChargeCoordinator)
                .chargeSubscription(anyLong(), anyLong(), any(Provider.class), anyString(), anyLong(), anyBoolean());
    }

    @Test
    @DisplayName("가상 스레드 병렬 청구가 순차 청구보다 빠름")
    void virtualThreadBillingIsFasterThanSequential() {
        long sequentialMs = measureSequentialBaseline();
        long parallelMs = measureRealScheduler();

        System.out.printf("%n대상 %d건 / PG 호출당 지연 %dms / 동시 호출 상한 %d%n",
                TARGET_COUNT, PG_LATENCY_MS, MAX_CONCURRENT_CHARGES);
        System.out.printf("순차 처리 (for문 기준선):            %,dms%n", sequentialMs);
        System.out.printf("가상 스레드 병렬 (runDailyBilling): %,dms%n", parallelMs);
        System.out.printf("개선 배수: %.1f배%n%n", sequentialMs / (double) parallelMs);

        assertThat(parallelMs).isLessThan(sequentialMs);
    }

    /** 가상 스레드 + Semaphore + Future.get() 경로를 전부 탄다. */
    private long measureRealScheduler() {
        long start = System.currentTimeMillis();
        subscriptionBillingScheduler.runDailyBilling();
        return System.currentTimeMillis() - start;
    }

    /** 병렬화 전: 같은 대상, 같은 mock을 for문으로 순차 호출. */
    private long measureSequentialBaseline() {
        List<BillingTarget> sequentialTargets = new ArrayList<>(targets);
        long start = System.currentTimeMillis();
        for (BillingTarget target : sequentialTargets) {
            try {
                subscriptionChargeCoordinator.chargeSubscription(target.subscriptionId(), target.userId(),
                        target.provider(), target.providerCredential(), target.amount(), target.wasRecovering());
            } catch (Exception e) {
                // 건별 실패는 흡수하고 계속 진행
            }
        }
        return System.currentTimeMillis() - start;
    }
}
