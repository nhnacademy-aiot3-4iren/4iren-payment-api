package com.siren.sirenpaymentapi.controller;

import com.siren.sirenpaymentapi.domain.Plan;
import com.siren.sirenpaymentapi.domain.PlanPriceStatus;
import com.siren.sirenpaymentapi.domain.entity.PlanPrices;
import com.siren.sirenpaymentapi.dto.plan_prices.PlanPriceResponse;
import com.siren.sirenpaymentapi.service.basic_service.PlanPricesService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanPriceControllerTest {

    @Mock
    private PlanPricesService planPricesService;

    @InjectMocks
    private PlanPriceController planPriceController;

    @Test
    void getCurrentPlanPricesReturnsList() {
        PlanPrices monthly = PlanPrices.builder().plan(Plan.MONTHLY).amount(29000L).status(PlanPriceStatus.ACTIVE).build();
        when(planPricesService.getAllCurrentPrices()).thenReturn(List.of(monthly));

        List<PlanPriceResponse> result = planPriceController.getCurrentPlanPrices();

        assertEquals(1, result.size());
        assertEquals(29000L, result.get(0).amount());
    }

    @Test
    void getCurrentPlanPricesReturnsEmptyList() {
        when(planPricesService.getAllCurrentPrices()).thenReturn(List.of());

        List<PlanPriceResponse> result = planPriceController.getCurrentPlanPrices();

        assertTrue(result.isEmpty());
    }
}
