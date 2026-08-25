package com.siren.sirenpaymentapi.domain.entity;

import com.siren.sirenpaymentapi.domain.Plan;
import com.siren.sirenpaymentapi.domain.PlanPriceStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlanPricesTest {

    @Test
    void markInactiveSetsStatus() {
        PlanPrices planPrice = PlanPrices.builder()
                .plan(Plan.MONTHLY)
                .amount(29000L)
                .status(PlanPriceStatus.ACTIVE)
                .build();

        planPrice.markInactive();

        assertEquals(PlanPriceStatus.INACTIVE, planPrice.getStatus());
    }
}
