package com.siren.sirenpaymentapi.gateway;

import com.siren.sirenpaymentapi.domain.Provider;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RecurringPaymentGatewayRegistryTest {

    @Test
    void getGatewayReturnsMatchingGateway() {
        RecurringPaymentGateway tossGateway = mock(RecurringPaymentGateway.class);
        when(tossGateway.getProvider()).thenReturn(Provider.TOSS_PAY);
        RecurringPaymentGatewayRegistry registry = new RecurringPaymentGatewayRegistry(List.of(tossGateway));

        assertEquals(tossGateway, registry.getGateway(Provider.TOSS_PAY));
    }

    @Test
    void getGatewayThrowsWhenNoMatch() {
        RecurringPaymentGatewayRegistry registry = new RecurringPaymentGatewayRegistry(List.of());

        assertThrows(IllegalArgumentException.class, () -> registry.getGateway(Provider.TOSS_PAY));
    }
}
