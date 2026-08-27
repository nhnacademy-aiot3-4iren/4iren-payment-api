package com.siren.sirenpaymentapi.service.basic_service;

import com.siren.sirenpaymentapi.domain.BillingKeyStatus;
import com.siren.sirenpaymentapi.domain.Provider;
import com.siren.sirenpaymentapi.domain.entity.BillingKeys;
import com.siren.sirenpaymentapi.exception.NotFoundBillingKeysException;
import com.siren.sirenpaymentapi.repository.BillingKeysRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillingKeysServiceTest {

    @Mock
    private BillingKeysRepository billingKeysRepository;

    @InjectMocks
    private BillingKeysService billingKeysService;

    @Test
    void findActiveByUserIdReturnsBillingKey() {
        BillingKeys billingKey = BillingKeys.builder().id(1L).userId(1L).status(BillingKeyStatus.ACTIVE).build();
        when(billingKeysRepository.findByUserIdAndStatus(1L, BillingKeyStatus.ACTIVE))
                .thenReturn(Optional.of(billingKey));

        Optional<BillingKeys> result = billingKeysService.findActiveByUserId(1L);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
    }

    @Test
    void findActiveByUserIdReturnsEmpty() {
        when(billingKeysRepository.findByUserIdAndStatus(1L, BillingKeyStatus.ACTIVE))
                .thenReturn(Optional.empty());

        Optional<BillingKeys> result = billingKeysService.findActiveByUserId(1L);

        assertTrue(result.isEmpty());
    }

    @Test
    void registerBillingKeysSavesActiveBillingKey() {
        when(billingKeysRepository.save(any(BillingKeys.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BillingKeys result = billingKeysService.registerBillingKeys(1L, Provider.TOSS_PAY, "credential", "CARD");

        assertEquals(BillingKeyStatus.ACTIVE, result.getStatus());
        assertEquals(1L, result.getUserId());
        assertEquals(Provider.TOSS_PAY, result.getProvider());
    }

    @Test
    void deleteBillingKeysMarksDeleted() {
        BillingKeys billingKey = BillingKeys.builder().id(1L).status(BillingKeyStatus.ACTIVE).build();
        when(billingKeysRepository.findById(1L)).thenReturn(Optional.of(billingKey));

        billingKeysService.deleteBillingKeys(1L);

        assertEquals(BillingKeyStatus.DELETED, billingKey.getStatus());
    }

    @Test
    void deleteBillingKeysThrowsWhenNotFound() {
        when(billingKeysRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundBillingKeysException.class, () -> billingKeysService.deleteBillingKeys(1L));
    }

    @Test
    void findPendingByUserIdReturnsBillingKey() {
        BillingKeys billingKey = BillingKeys.builder().id(1L).userId(1L).status(BillingKeyStatus.PENDING).build();
        when(billingKeysRepository.findByUserIdAndStatus(1L, BillingKeyStatus.PENDING))
                .thenReturn(Optional.of(billingKey));

        Optional<BillingKeys> result = billingKeysService.findPendingByUserId(1L);

        assertTrue(result.isPresent());
    }

    @Test
    void registerPendingBillingKeySavesPendingBillingKey() {
        when(billingKeysRepository.findByUserIdAndStatus(1L, BillingKeyStatus.PENDING)).thenReturn(Optional.empty());
        when(billingKeysRepository.save(any(BillingKeys.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BillingKeys result = billingKeysService.registerPendingBillingKey(1L, Provider.TOSS_PAY, "credential", "CARD");

        assertEquals(BillingKeyStatus.PENDING, result.getStatus());
        assertEquals(1L, result.getUserId());
    }

    @Test
    void registerPendingBillingKeyDiscardsExistingPending() {
        BillingKeys existingPending = BillingKeys.builder().id(9L).status(BillingKeyStatus.PENDING).build();
        when(billingKeysRepository.findByUserIdAndStatus(1L, BillingKeyStatus.PENDING))
                .thenReturn(Optional.of(existingPending));
        when(billingKeysRepository.save(any(BillingKeys.class))).thenAnswer(invocation -> invocation.getArgument(0));

        billingKeysService.registerPendingBillingKey(1L, Provider.TOSS_PAY, "credential", "CARD");

        assertEquals(BillingKeyStatus.DELETED, existingPending.getStatus());
    }

    @Test
    void activateBillingKeyMarksActive() {
        BillingKeys billingKey = BillingKeys.builder().id(1L).status(BillingKeyStatus.PENDING).build();
        when(billingKeysRepository.findById(1L)).thenReturn(Optional.of(billingKey));

        billingKeysService.activateBillingKey(1L);

        assertEquals(BillingKeyStatus.ACTIVE, billingKey.getStatus());
    }

    @Test
    void activateBillingKeyThrowsWhenNotFound() {
        when(billingKeysRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundBillingKeysException.class, () -> billingKeysService.activateBillingKey(1L));
    }
}
