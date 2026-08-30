package com.siren.sirenpaymentapi.service;

import com.siren.sirenpaymentapi.domain.Plan;
import com.siren.sirenpaymentapi.domain.Provider;
import com.siren.sirenpaymentapi.domain.entity.BillingKeys;
import com.siren.sirenpaymentapi.domain.entity.PlanPrices;
import com.siren.sirenpaymentapi.domain.entity.Subscriptions;
import com.siren.sirenpaymentapi.dto.billing_keys.ConfirmRegistrationCommand;
import com.siren.sirenpaymentapi.dto.subscriptions.BillingTarget;
import com.siren.sirenpaymentapi.service.basic_service.BillingKeysService;
import com.siren.sirenpaymentapi.service.basic_service.PlanPricesService;
import com.siren.sirenpaymentapi.service.basic_service.SubscriptionsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationConfirmationServiceTest {

    @Mock
    private BillingKeysService billingKeysService;

    @Mock
    private SubscriptionsService subscriptionsService;

    @Mock
    private PlanPricesService planPricesService;

    @InjectMocks
    private RegistrationConfirmationService registrationConfirmationService;

    @Test
    void confirmRegistrationCreatesBillingKeyAndSubscription() {
        BillingKeys billingKeys = BillingKeys.builder().id(1L).build();
        PlanPrices planPrice = PlanPrices.builder().id(1L).build();
        Subscriptions subscription = Subscriptions.builder().id(10L).build();
        when(billingKeysService.registerBillingKeys(1L, Provider.TOSS_PAY, "credential", "CARD"))
                .thenReturn(billingKeys);
        when(planPricesService.getReference(1L)).thenReturn(planPrice);
        when(subscriptionsService.registerSubscription(1L, billingKeys, planPrice, Plan.MONTHLY, 29000L))
                .thenReturn(subscription);

        BillingTarget target = registrationConfirmationService.confirmRegistration(new ConfirmRegistrationCommand(
                1L, Provider.TOSS_PAY, "credential", "CARD", Plan.MONTHLY, 29000L, 1L, "token-1"));

        assertEquals(10L, target.subscriptionId());
        assertEquals(1L, target.userId());
        assertEquals(Provider.TOSS_PAY, target.provider());
        assertEquals(29000L, target.amount());
        assertEquals(false, target.wasRecovering());
    }
}
