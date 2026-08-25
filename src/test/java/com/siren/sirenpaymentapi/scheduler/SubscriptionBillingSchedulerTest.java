package com.siren.sirenpaymentapi.scheduler;

import com.siren.sirenpaymentapi.domain.Provider;
import com.siren.sirenpaymentapi.dto.subscriptions.BillingTarget;
import com.siren.sirenpaymentapi.service.SubscriptionChargeCoordinator;
import com.siren.sirenpaymentapi.service.basic_service.SubscriptionsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionBillingSchedulerTest {

    @Mock
    private SubscriptionsService subscriptionsService;

    @Mock
    private SubscriptionChargeCoordinator subscriptionChargeCoordinator;

    @InjectMocks
    private SubscriptionBillingScheduler subscriptionBillingScheduler;

    @BeforeEach
    void setMaxConcurrentCharges() {
        ReflectionTestUtils.setField(subscriptionBillingScheduler, "maxConcurrentCharges", 10);
    }

    @Test
    void runDailyBillingChargesAllTargets() {
        BillingTarget target = new BillingTarget(1L, 2L, Provider.TOSS_PAY, "credential", 29000L, false);
        when(subscriptionsService.findDueForBilling(any(LocalDate.class))).thenReturn(List.of(target));

        subscriptionBillingScheduler.runDailyBilling();

        verify(subscriptionChargeCoordinator).chargeSubscription(1L, 2L, Provider.TOSS_PAY, "credential", 29000L, false);
    }

    @Test
    void runDailyBillingContinuesWhenOneTargetFails() {
        BillingTarget failing = new BillingTarget(1L, 2L, Provider.TOSS_PAY, "credential", 29000L, false);
        BillingTarget ok = new BillingTarget(3L, 4L, Provider.KAKAO_PAY, "credential2", 29000L, false);
        when(subscriptionsService.findDueForBilling(any(LocalDate.class))).thenReturn(List.of(failing, ok));
        doThrow(new RuntimeException("결제 실패"))
                .when(subscriptionChargeCoordinator).chargeSubscription(1L, 2L, Provider.TOSS_PAY, "credential", 29000L, false);

        subscriptionBillingScheduler.runDailyBilling();

        verify(subscriptionChargeCoordinator).chargeSubscription(3L, 4L, Provider.KAKAO_PAY, "credential2", 29000L, false);
    }

    @Test
    void runDailyBillingDoesNothingWhenNoTargets() {
        when(subscriptionsService.findDueForBilling(any(LocalDate.class))).thenReturn(List.of());

        subscriptionBillingScheduler.runDailyBilling();

        verifyNoInteractions(subscriptionChargeCoordinator);
    }
}
