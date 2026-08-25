package com.siren.sirenpaymentapi.service.basic_service;

import com.siren.sirenpaymentapi.domain.Plan;
import com.siren.sirenpaymentapi.domain.PlanPriceStatus;
import com.siren.sirenpaymentapi.domain.entity.PlanPrices;
import com.siren.sirenpaymentapi.exception.NotFoundPlanPriceException;
import com.siren.sirenpaymentapi.repository.PlanPricesRepository;
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
class PlanPricesServiceTest {

    @Mock
    private PlanPricesRepository planPricesRepository;

    @InjectMocks
    private PlanPricesService planPricesService;

    @Test
    void getAllCurrentPricesReturnsActiveOnly() {
        PlanPrices monthly = PlanPrices.builder().plan(Plan.MONTHLY).amount(29000L).status(PlanPriceStatus.ACTIVE).build();
        when(planPricesRepository.findByStatus(PlanPriceStatus.ACTIVE)).thenReturn(List.of(monthly));

        List<PlanPrices> result = planPricesService.getAllCurrentPrices();

        assertEquals(1, result.size());
    }

    @Test
    void getCurrentPlanPriceReturnsPrice() {
        PlanPrices monthly = PlanPrices.builder().plan(Plan.MONTHLY).amount(29000L).status(PlanPriceStatus.ACTIVE).build();
        when(planPricesRepository.findByPlanAndStatus(Plan.MONTHLY, PlanPriceStatus.ACTIVE))
                .thenReturn(Optional.of(monthly));

        PlanPrices result = planPricesService.getCurrentPlanPrice(Plan.MONTHLY);

        assertEquals(29000L, result.getAmount());
    }

    @Test
    void getCurrentPlanPriceThrowsWhenMissing() {
        when(planPricesRepository.findByPlanAndStatus(Plan.MONTHLY, PlanPriceStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundPlanPriceException.class, () -> planPricesService.getCurrentPlanPrice(Plan.MONTHLY));
    }

    @Test
    void changePriceMarksOldPriceInactive() {
        PlanPrices oldPrice = PlanPrices.builder().plan(Plan.MONTHLY).amount(29000L).status(PlanPriceStatus.ACTIVE).build();
        when(planPricesRepository.findByPlanAndStatus(Plan.MONTHLY, PlanPriceStatus.ACTIVE))
                .thenReturn(Optional.of(oldPrice));

        planPricesService.changePrice(Plan.MONTHLY, 39000L);

        assertEquals(PlanPriceStatus.INACTIVE, oldPrice.getStatus());
        verify(planPricesRepository).save(any(PlanPrices.class));
    }

    @Test
    void changePriceSavesNewActivePrice() {
        when(planPricesRepository.findByPlanAndStatus(Plan.MONTHLY, PlanPriceStatus.ACTIVE))
                .thenReturn(Optional.empty());

        planPricesService.changePrice(Plan.MONTHLY, 39000L);

        verify(planPricesRepository).save(argThat(planPrice ->
                planPrice.getAmount().equals(39000L) && planPrice.getStatus() == PlanPriceStatus.ACTIVE));
    }
}
