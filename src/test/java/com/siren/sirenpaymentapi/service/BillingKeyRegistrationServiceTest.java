package com.siren.sirenpaymentapi.service;

import com.siren.sirenpaymentapi.client.CoreApiClient;
import com.siren.sirenpaymentapi.domain.BillingKeyStatus;
import com.siren.sirenpaymentapi.domain.Plan;
import com.siren.sirenpaymentapi.domain.Provider;
import com.siren.sirenpaymentapi.domain.SubscriptionStatus;
import com.siren.sirenpaymentapi.domain.entity.BillingKeys;
import com.siren.sirenpaymentapi.domain.entity.PlanPrices;
import com.siren.sirenpaymentapi.domain.entity.Subscriptions;
import com.siren.sirenpaymentapi.dto.billing_keys.ConfirmRegistrationCommand;
import com.siren.sirenpaymentapi.dto.core.TeamCheckRequest;
import com.siren.sirenpaymentapi.dto.core.TeamCheckResponse;
import com.siren.sirenpaymentapi.event.RoleChangeRequested;
import com.siren.sirenpaymentapi.exception.AlreadyBelongsToTeamException;
import com.siren.sirenpaymentapi.exception.NotFoundBillingKeysException;
import com.siren.sirenpaymentapi.exception.NotFoundSubscriptionException;
import com.siren.sirenpaymentapi.gateway.RecurringPaymentGateway;
import com.siren.sirenpaymentapi.gateway.RecurringPaymentGatewayRegistry;
import com.siren.sirenpaymentapi.service.basic_service.BillingKeysService;
import com.siren.sirenpaymentapi.service.basic_service.PlanPricesService;
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
    private PlanPricesService planPricesService;

    @Mock
    private RecurringPaymentGatewayRegistry gatewayRegistry;

    @Mock
    private RoleChangeEventPublisher roleChangeEventPublisher;

    @Mock
    private CoreApiClient coreApiClient;

    @Mock
    private RecurringPaymentGateway gateway;

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
    void confirmRegistrationPublishesOwnerEvent() {
        BillingKeys billingKeys = BillingKeys.builder().id(1L).build();
        PlanPrices planPrice = PlanPrices.builder().id(1L).build();
        when(billingKeysService.registerBillingKeys(1L, Provider.TOSS_PAY, "credential", "CARD"))
                .thenReturn(billingKeys);
        when(planPricesService.getReference(1L)).thenReturn(planPrice);

        billingKeyRegistrationService.confirmRegistration(new ConfirmRegistrationCommand(
                1L, Provider.TOSS_PAY, "credential", "CARD", Plan.MONTHLY, 29000L, 1L, "token-1"));

        verify(subscriptionsService).registerSubscription(1L, billingKeys, planPrice, Plan.MONTHLY, 29000L);
        verify(roleChangeEventPublisher).requestRoleChange(1L, RoleChangeRequested.OWNER, "token-1");
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
