package com.siren.sirenpaymentapi.scheduler;

import com.siren.sirenpaymentapi.service.basic_service.SubscriptionsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CanceledSubscriptionExpirySchedulerTest {

    @Mock
    private SubscriptionsService subscriptionsService;

    @InjectMocks
    private CanceledSubscriptionExpiryScheduler canceledSubscriptionExpiryScheduler;

    @Test
    void expireCanceledSubscriptionsDowngradesEachSubscription() {
        when(subscriptionsService.findCanceledPastPeriodEnd(any(LocalDateTime.class))).thenReturn(List.of(1L, 2L));

        canceledSubscriptionExpiryScheduler.expireCanceledSubscriptions();

        verify(subscriptionsService).downgradeAfterCancelation(1L);
        verify(subscriptionsService).downgradeAfterCancelation(2L);
    }

    @Test
    void expireCanceledSubscriptionsContinuesWhenOneFails() {
        when(subscriptionsService.findCanceledPastPeriodEnd(any(LocalDateTime.class))).thenReturn(List.of(1L, 2L));
        doThrow(new RuntimeException("실패")).when(subscriptionsService).downgradeAfterCancelation(1L);

        canceledSubscriptionExpiryScheduler.expireCanceledSubscriptions();

        verify(subscriptionsService).downgradeAfterCancelation(2L);
    }

    @Test
    void expireCanceledSubscriptionsDoesNothingWhenEmpty() {
        when(subscriptionsService.findCanceledPastPeriodEnd(any(LocalDateTime.class))).thenReturn(List.of());

        canceledSubscriptionExpiryScheduler.expireCanceledSubscriptions();

        verify(subscriptionsService, never()).downgradeAfterCancelation(any());
    }
}
