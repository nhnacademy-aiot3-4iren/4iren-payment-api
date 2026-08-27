package com.siren.sirenpaymentapi.service;

import com.siren.sirenpaymentapi.domain.Provider;
import com.siren.sirenpaymentapi.dto.gateway.ActiveBillingKey;
import com.siren.sirenpaymentapi.dto.gateway.ChargeResult;
import com.siren.sirenpaymentapi.dto.payments.PreparedCharge;
import com.siren.sirenpaymentapi.gateway.RecurringPaymentGateway;
import com.siren.sirenpaymentapi.gateway.RecurringPaymentGatewayRegistry;
import com.siren.sirenpaymentapi.service.basic_service.PaymentsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionChargeCoordinatorTest {

    @Mock
    private PaymentsService paymentsService;

    @Mock
    private SubscriptionChargeService subscriptionChargeService;

    @Mock
    private BillingKeyRegistrationService billingKeyRegistrationService;

    @Mock
    private RecurringPaymentGatewayRegistry gatewayRegistry;

    @Mock
    private RecurringPaymentGateway gateway;

    @InjectMocks
    private SubscriptionChargeCoordinator subscriptionChargeCoordinator;

    @Test
    void chargeSubscriptionRecordsSuccess() {
        when(billingKeyRegistrationService.applyPendingBillingKeyIfAny(2L, 1L, Provider.TOSS_PAY, "credential"))
                .thenReturn(new ActiveBillingKey(Provider.TOSS_PAY, "credential"));
        when(paymentsService.prepareCharge(1L, 29000L)).thenReturn(new PreparedCharge(10L, "order-1"));
        when(gatewayRegistry.getGateway(Provider.TOSS_PAY)).thenReturn(gateway);
        when(gateway.charge("credential", 29000L, "order-1"))
                .thenReturn(ChargeResult.success("tx-1", "pay-token-1", "{}"));

        subscriptionChargeCoordinator.chargeSubscription(1L, 2L, Provider.TOSS_PAY, "credential", 29000L, false);

        verify(subscriptionChargeService).recordSuccess(10L, 1L, false, "tx-1", "pay-token-1", "{}");
    }

    @Test
    void chargeSubscriptionRecordsFailure() {
        when(billingKeyRegistrationService.applyPendingBillingKeyIfAny(2L, 1L, Provider.TOSS_PAY, "credential"))
                .thenReturn(new ActiveBillingKey(Provider.TOSS_PAY, "credential"));
        when(paymentsService.prepareCharge(1L, 29000L)).thenReturn(new PreparedCharge(10L, "order-1"));
        when(gatewayRegistry.getGateway(Provider.TOSS_PAY)).thenReturn(gateway);
        when(gateway.charge("credential", 29000L, "order-1"))
                .thenReturn(ChargeResult.failure("카드 한도 초과", "{}"));

        subscriptionChargeCoordinator.chargeSubscription(1L, 2L, Provider.TOSS_PAY, "credential", 29000L, false);

        verify(subscriptionChargeService).recordFailure(10L, 1L, "카드 한도 초과", "{}");
    }

    @Test
    void chargeSubscriptionRevokesBillingKeyWhenSubscriptionExpires() {
        when(billingKeyRegistrationService.applyPendingBillingKeyIfAny(2L, 1L, Provider.TOSS_PAY, "credential"))
                .thenReturn(new ActiveBillingKey(Provider.TOSS_PAY, "credential"));
        when(paymentsService.prepareCharge(1L, 29000L)).thenReturn(new PreparedCharge(10L, "order-1"));
        when(gatewayRegistry.getGateway(Provider.TOSS_PAY)).thenReturn(gateway);
        when(gateway.charge("credential", 29000L, "order-1"))
                .thenReturn(ChargeResult.failure("카드 한도 초과", "{}"));
        when(subscriptionChargeService.recordFailure(10L, 1L, "카드 한도 초과", "{}")).thenReturn(true);

        subscriptionChargeCoordinator.chargeSubscription(1L, 2L, Provider.TOSS_PAY, "credential", 29000L, false);

        verify(billingKeyRegistrationService).revokeBillingKeyAfterExpiry(2L);
    }

    @Test
    void chargeSubscriptionDoesNotRevokeWhenNotExpired() {
        when(billingKeyRegistrationService.applyPendingBillingKeyIfAny(2L, 1L, Provider.TOSS_PAY, "credential"))
                .thenReturn(new ActiveBillingKey(Provider.TOSS_PAY, "credential"));
        when(paymentsService.prepareCharge(1L, 29000L)).thenReturn(new PreparedCharge(10L, "order-1"));
        when(gatewayRegistry.getGateway(Provider.TOSS_PAY)).thenReturn(gateway);
        when(gateway.charge("credential", 29000L, "order-1"))
                .thenReturn(ChargeResult.failure("카드 한도 초과", "{}"));
        when(subscriptionChargeService.recordFailure(10L, 1L, "카드 한도 초과", "{}")).thenReturn(false);

        subscriptionChargeCoordinator.chargeSubscription(1L, 2L, Provider.TOSS_PAY, "credential", 29000L, false);

        verify(billingKeyRegistrationService, never()).revokeBillingKeyAfterExpiry(any());
    }

    @Test
    void chargeSubscriptionRevokesBillingKeyWhenGatewayReportsRevoked() {
        when(billingKeyRegistrationService.applyPendingBillingKeyIfAny(2L, 1L, Provider.TOSS_PAY, "credential"))
                .thenReturn(new ActiveBillingKey(Provider.TOSS_PAY, "credential"));
        when(paymentsService.prepareCharge(1L, 29000L)).thenReturn(new PreparedCharge(10L, "order-1"));
        when(gatewayRegistry.getGateway(Provider.TOSS_PAY)).thenReturn(gateway);
        when(gateway.charge("credential", 29000L, "order-1"))
                .thenReturn(ChargeResult.billingKeyRevoked("빌링키 없음", "{}"));

        subscriptionChargeCoordinator.chargeSubscription(1L, 2L, Provider.TOSS_PAY, "credential", 29000L, false);

        verify(subscriptionChargeService).recordFailureFromRevokedBillingKey(10L, "빌링키 없음", "{}");
        verify(billingKeyRegistrationService).revokeByProviderNotice(2L);
        verify(subscriptionChargeService, never()).recordFailure(any(), any(), any(), any());
    }

    @Test
    void chargeSubscriptionUsesSwappedBillingKeyWhenPendingExists() {
        when(billingKeyRegistrationService.applyPendingBillingKeyIfAny(2L, 1L, Provider.TOSS_PAY, "old-credential"))
                .thenReturn(new ActiveBillingKey(Provider.KAKAO_PAY, "new-credential"));
        when(paymentsService.prepareCharge(1L, 29000L)).thenReturn(new PreparedCharge(10L, "order-1"));
        when(gatewayRegistry.getGateway(Provider.KAKAO_PAY)).thenReturn(gateway);
        when(gateway.charge("new-credential", 29000L, "order-1"))
                .thenReturn(ChargeResult.success("tx-1", "pay-token-1", "{}"));

        subscriptionChargeCoordinator.chargeSubscription(1L, 2L, Provider.TOSS_PAY, "old-credential", 29000L, false);

        verify(gateway).charge("new-credential", 29000L, "order-1");
        verify(gatewayRegistry, never()).getGateway(Provider.TOSS_PAY);
        verify(subscriptionChargeService).recordSuccess(10L, 1L, false, "tx-1", "pay-token-1", "{}");
    }
}
