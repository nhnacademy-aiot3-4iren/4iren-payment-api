package com.siren.sirenpaymentapi.domain.entity;

import com.siren.sirenpaymentapi.domain.BillingKeyStatus;
import com.siren.sirenpaymentapi.domain.Provider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BillingKeysTest {

    @Test
    void markDeletedSetsStatusAndDeletedAt() {
        BillingKeys billingKey = BillingKeys.builder()
                .userId(1L)
                .provider(Provider.TOSS_PAY)
                .providerCredential("credential")
                .status(BillingKeyStatus.ACTIVE)
                .build();

        billingKey.markDeleted();

        assertEquals(BillingKeyStatus.DELETED, billingKey.getStatus());
        assertNotNull(billingKey.getDeletedAt());
    }
}
