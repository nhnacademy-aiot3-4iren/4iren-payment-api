package com.siren.sirenpaymentapi.service;

import com.siren.sirenpaymentapi.client.CoreApiClient;
import com.siren.sirenpaymentapi.domain.BillingKeyStatus;
import com.siren.sirenpaymentapi.domain.Plan;
import com.siren.sirenpaymentapi.domain.Provider;
import com.siren.sirenpaymentapi.domain.SubscriptionStatus;
import com.siren.sirenpaymentapi.domain.entity.BillingKeys;
import com.siren.sirenpaymentapi.domain.entity.Subscriptions;
import com.siren.sirenpaymentapi.dto.billing_keys.ConfirmRegistrationCommand;
import com.siren.sirenpaymentapi.dto.core.TeamCheckRequest;
import com.siren.sirenpaymentapi.dto.core.TeamCheckResponse;
import com.siren.sirenpaymentapi.dto.gateway.ActiveBillingKey;
import com.siren.sirenpaymentapi.dto.gateway.ChargeResult;
import com.siren.sirenpaymentapi.dto.payments.PreparedCharge;
import com.siren.sirenpaymentapi.dto.subscriptions.BillingTarget;
import com.siren.sirenpaymentapi.event.RoleChangeRequested;
import com.siren.sirenpaymentapi.exception.AlreadyBelongsToTeamException;
import com.siren.sirenpaymentapi.exception.InitialChargeFailedException;
import com.siren.sirenpaymentapi.exception.NotFoundBillingKeysException;
import com.siren.sirenpaymentapi.exception.NotFoundSubscriptionException;
import com.siren.sirenpaymentapi.exception.SameProviderBillingKeyChangeException;
import com.siren.sirenpaymentapi.gateway.RecurringPaymentGateway;
import com.siren.sirenpaymentapi.gateway.RecurringPaymentGatewayRegistry;
import com.siren.sirenpaymentapi.mail.MailEventPublisher;
import com.siren.sirenpaymentapi.service.basic_service.BillingKeysService;
import com.siren.sirenpaymentapi.service.basic_service.PaymentsService;
import com.siren.sirenpaymentapi.service.basic_service.SubscriptionsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillingKeyRegistrationServiceTest {

    @Mock
    private BillingKeysService billingKeysService;

    @Mock
    private SubscriptionsService subscriptionsService;

    @Mock
    private PaymentsService paymentsService;

    @Mock
    private SubscriptionChargeService subscriptionChargeService;

    @Mock
    private RecurringPaymentGatewayRegistry gatewayRegistry;

    @Mock
    private RoleChangeEventPublisher roleChangeEventPublisher;

    @Mock
    private CoreApiClient coreApiClient;

    @Mock
    private MailEventPublisher mailEventPublisher;

    @Mock
    private RecurringPaymentGateway gateway;

    @Mock
    private RegistrationConfirmationService registrationConfirmationService;

    @InjectMocks
    private BillingKeyRegistrationService billingKeyRegistrationService;

    @Test
    void verifyEligibleForRegistrationSkipsWhenHasHistory() {
        when(subscriptionsService.hasSubscriptionHistory(1L)).thenReturn(true);

        billingKeyRegistrationService.verifyEligibleForRegistration(1L);

        verifyNoInteractions(coreApiClient);
    }

    @Test
    void verifyEligibleForRegistrationPassesWhenNoTeam() {
        when(subscriptionsService.hasSubscriptionHistory(1L)).thenReturn(false);
        when(coreApiClient.checkTeams(new TeamCheckRequest(1L)))
                .thenReturn(new TeamCheckResponse(1L, List.of()));

        assertDoesNotThrow(() -> billingKeyRegistrationService.verifyEligibleForRegistration(1L));
    }

    @Test
    void verifyEligibleForRegistrationThrowsWhenAlreadyInTeam() {
        when(subscriptionsService.hasSubscriptionHistory(1L)).thenReturn(false);
        when(coreApiClient.checkTeams(new TeamCheckRequest(1L)))
                .thenReturn(new TeamCheckResponse(1L, List.of(10L)));

        assertThrows(AlreadyBelongsToTeamException.class,
                () -> billingKeyRegistrationService.verifyEligibleForRegistration(1L));
    }

    @Test
    void confirmRegistrationAndChargePublishesOwnerEventWhenChargeSucceeds() {
        Subscriptions subscription = Subscriptions.builder().id(10L).build();
        BillingTarget target = new BillingTarget(10L, 1L, Provider.TOSS_PAY, "credential", 29000L, false);
        when(registrationConfirmationService.confirmRegistration(any())).thenReturn(target);
        when(paymentsService.prepareCharge(10L, 29000L)).thenReturn(new PreparedCharge(100L, "order-1"));
        when(gatewayRegistry.getGateway(Provider.TOSS_PAY)).thenReturn(gateway);
        when(gateway.charge("credential", 29000L, "order-1"))
                .thenReturn(ChargeResult.success("txn-1", "pay-1", "raw"));
        when(subscriptionsService.getById(10L)).thenReturn(subscription);

        billingKeyRegistrationService.confirmRegistrationAndCharge(new ConfirmRegistrationCommand(
                1L, Provider.TOSS_PAY, "credential", "CARD", Plan.MONTHLY, 29000L, 1L, "token-1"));

        verify(subscriptionChargeService).recordInitialChargeSuccess(100L, "txn-1", "pay-1", "raw");
        verify(roleChangeEventPublisher).requestRoleChange(1L, RoleChangeRequested.OWNER, "token-1");
        verify(mailEventPublisher).notify(eq(1L), any());
    }

    @Test
    void confirmRegistrationAndChargeFailsRegistrationWhenChargeFails() {
        BillingKeys billingKeys = BillingKeys.builder().id(1L).build();
        BillingTarget target = new BillingTarget(10L, 1L, Provider.TOSS_PAY, "credential", 29000L, false);
        when(registrationConfirmationService.confirmRegistration(any())).thenReturn(target);
        when(paymentsService.prepareCharge(10L, 29000L)).thenReturn(new PreparedCharge(100L, "order-1"));
        when(gatewayRegistry.getGateway(Provider.TOSS_PAY)).thenReturn(gateway);
        when(gateway.charge("credential", 29000L, "order-1"))
                .thenReturn(ChargeResult.failure("카드 한도 초과", "raw"));
        when(billingKeysService.findActiveByUserId(1L)).thenReturn(Optional.of(billingKeys));

        ConfirmRegistrationCommand command = new ConfirmRegistrationCommand(
                1L, Provider.TOSS_PAY, "credential", "CARD", Plan.MONTHLY, 29000L, 1L, "token-1");

        assertThrows(InitialChargeFailedException.class, () ->
                billingKeyRegistrationService.confirmRegistrationAndCharge(command));

        verify(subscriptionChargeService).recordInitialChargeFailure(100L, 10L, "카드 한도 초과", "raw");
        verify(gateway).revoke("credential");
        verify(billingKeysService).deleteBillingKeys(1L);
        verifyNoInteractions(roleChangeEventPublisher);
    }

    @Test
    void changeBillingKeyReplacesOldKey() {
        BillingKeys newKey = BillingKeys.builder().id(2L).build();
        when(billingKeysService.registerBillingKeys(1L, Provider.TOSS_PAY, "new-credential", "CARD"))
                .thenReturn(newKey);

        billingKeyRegistrationService.changeBillingKey(1L, 99L, 1L, Provider.TOSS_PAY, "new-credential", "CARD");

        verify(billingKeysService).deleteBillingKeys(99L);
        verify(subscriptionsService).replaceBillingKey(1L, newKey);
    }

    @Test
    void applyPendingBillingKeyIfAnyReturnsCurrentWhenNoPending() {
        when(billingKeysService.findPendingByUserId(1L)).thenReturn(Optional.empty());

        ActiveBillingKey result = billingKeyRegistrationService.applyPendingBillingKeyIfAny(
                1L, 2L, Provider.TOSS_PAY, "old-credential");

        assertEquals(new ActiveBillingKey(Provider.TOSS_PAY, "old-credential"), result);
        verify(billingKeysService, never()).activateBillingKey(any());
        verify(subscriptionsService, never()).replaceBillingKey(any(), any());
    }

    @Test
    void applyPendingBillingKeyIfAnySwapsWhenPendingExists() {
        BillingKeys oldKey = BillingKeys.builder().id(1L).provider(Provider.TOSS_PAY).providerCredential("old-credential").build();
        BillingKeys pendingKey = BillingKeys.builder().id(2L).provider(Provider.KAKAO_PAY).providerCredential("new-credential").build();
        when(billingKeysService.findPendingByUserId(1L)).thenReturn(Optional.of(pendingKey));
        when(billingKeysService.findActiveByUserId(1L)).thenReturn(Optional.of(oldKey));
        when(gatewayRegistry.getGateway(Provider.TOSS_PAY)).thenReturn(gateway);

        ActiveBillingKey result = billingKeyRegistrationService.applyPendingBillingKeyIfAny(
                1L, 3L, Provider.TOSS_PAY, "old-credential");

        assertEquals(new ActiveBillingKey(Provider.KAKAO_PAY, "new-credential"), result);
        verify(gateway).revoke("old-credential");
        verify(billingKeysService).deleteBillingKeys(1L);
        verify(billingKeysService).activateBillingKey(2L);
        verify(subscriptionsService).replaceBillingKey(3L, pendingKey);
    }

    @Test
    void applyPendingBillingKeyIfAnySwapsWithoutDeletingWhenNoActiveKey() {
        BillingKeys pendingKey = BillingKeys.builder().id(2L).provider(Provider.KAKAO_PAY).providerCredential("new-credential").build();
        when(billingKeysService.findPendingByUserId(1L)).thenReturn(Optional.of(pendingKey));
        when(billingKeysService.findActiveByUserId(1L)).thenReturn(Optional.empty());

        billingKeyRegistrationService.applyPendingBillingKeyIfAny(1L, 3L, Provider.TOSS_PAY, "old-credential");

        verify(billingKeysService, never()).deleteBillingKeys(any());
        verify(billingKeysService).activateBillingKey(2L);
    }

    @Test
    void verifyEligibleForBillingKeyChangeReturnsActiveKeyWhenDifferentProvider() {
        BillingKeys active = BillingKeys.builder().id(1L).provider(Provider.TOSS_PAY).build();
        when(billingKeysService.findActiveByUserId(1L)).thenReturn(Optional.of(active));

        BillingKeys result = billingKeyRegistrationService.verifyEligibleForBillingKeyChange(1L, Provider.KAKAO_PAY);

        assertEquals(active, result);
    }

    @Test
    void verifyEligibleForBillingKeyChangeThrowsWhenSameProvider() {
        BillingKeys active = BillingKeys.builder().id(1L).provider(Provider.TOSS_PAY).build();
        when(billingKeysService.findActiveByUserId(1L)).thenReturn(Optional.of(active));

        assertThrows(SameProviderBillingKeyChangeException.class,
                () -> billingKeyRegistrationService.verifyEligibleForBillingKeyChange(1L, Provider.TOSS_PAY));
    }

    @Test
    void verifyEligibleForBillingKeyChangeThrowsWhenNoActiveKey() {
        when(billingKeysService.findActiveByUserId(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundBillingKeysException.class,
                () -> billingKeyRegistrationService.verifyEligibleForBillingKeyChange(1L, Provider.TOSS_PAY));
    }

    @Test
    void revokeBillingKeyAfterExpiryRevokesActiveKey() {
        BillingKeys active = BillingKeys.builder().id(1L).provider(Provider.TOSS_PAY).providerCredential("credential").build();
        when(billingKeysService.findActiveByUserId(1L)).thenReturn(Optional.of(active));
        when(gatewayRegistry.getGateway(Provider.TOSS_PAY)).thenReturn(gateway);

        billingKeyRegistrationService.revokeBillingKeyAfterExpiry(1L);

        verify(gateway).revoke("credential");
        verify(billingKeysService).deleteBillingKeys(1L);
    }

    @Test
    void revokeBillingKeyAfterExpiryDoesNothingWhenNoActiveKey() {
        when(billingKeysService.findActiveByUserId(1L)).thenReturn(Optional.empty());

        billingKeyRegistrationService.revokeBillingKeyAfterExpiry(1L);

        verifyNoInteractions(gatewayRegistry);
        verify(billingKeysService, never()).deleteBillingKeys(any());
    }

    @Test
    void revokeByProviderNoticeDeletesBillingKeyAndCancelsSubscription() {
        BillingKeys billingKeys = BillingKeys.builder().id(1L).status(BillingKeyStatus.ACTIVE).build();
        Subscriptions subscription = Subscriptions.builder().id(2L).status(SubscriptionStatus.ACTIVE).build();
        when(billingKeysService.findActiveByUserId(1L)).thenReturn(Optional.of(billingKeys));
        when(subscriptionsService.findActiveByUserId(1L)).thenReturn(Optional.of(subscription));

        billingKeyRegistrationService.revokeByProviderNotice(1L);

        verify(billingKeysService).deleteBillingKeys(1L);
        verify(subscriptionsService).markCanceled(2L);
    }

    @Test
    void revokeByProviderNoticeDoesNothingWhenNoActiveData() {
        when(billingKeysService.findActiveByUserId(1L)).thenReturn(Optional.empty());
        when(subscriptionsService.findActiveByUserId(1L)).thenReturn(Optional.empty());

        billingKeyRegistrationService.revokeByProviderNotice(1L);

        verify(billingKeysService, never()).deleteBillingKeys(any());
        verify(subscriptionsService, never()).markCanceled(any());
    }

    @Test
    void cancelSubscriptionRevokesAtGatewayAndMarksCanceled() {
        BillingKeys billingKeys = BillingKeys.builder().id(1L).provider(Provider.TOSS_PAY).providerCredential("credential").build();
        Subscriptions subscription = Subscriptions.builder().id(2L).build();
        when(billingKeysService.findActiveByUserId(1L)).thenReturn(Optional.of(billingKeys));
        when(subscriptionsService.findActiveByUserId(1L)).thenReturn(Optional.of(subscription));
        when(gatewayRegistry.getGateway(Provider.TOSS_PAY)).thenReturn(gateway);

        billingKeyRegistrationService.cancelSubscription(1L);

        verify(gateway).revoke("credential");
        verify(billingKeysService).deleteBillingKeys(1L);
        verify(subscriptionsService).markCanceled(2L);
    }

    @Test
    void cancelSubscriptionThrowsWhenNoBillingKey() {
        when(billingKeysService.findActiveByUserId(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundBillingKeysException.class, () -> billingKeyRegistrationService.cancelSubscription(1L));
    }

    @Test
    void cancelSubscriptionThrowsWhenNoSubscription() {
        BillingKeys billingKeys = BillingKeys.builder().id(1L).build();
        when(billingKeysService.findActiveByUserId(1L)).thenReturn(Optional.of(billingKeys));
        when(subscriptionsService.findActiveByUserId(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundSubscriptionException.class, () -> billingKeyRegistrationService.cancelSubscription(1L));
    }
}
