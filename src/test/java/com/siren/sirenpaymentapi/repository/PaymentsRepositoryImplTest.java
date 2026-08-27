package com.siren.sirenpaymentapi.repository;

import com.siren.sirenpaymentapi.config.QueryDslConfig;
import com.siren.sirenpaymentapi.domain.BillingKeyStatus;
import com.siren.sirenpaymentapi.domain.Plan;
import com.siren.sirenpaymentapi.domain.PaymentStatus;
import com.siren.sirenpaymentapi.domain.PlanPriceStatus;
import com.siren.sirenpaymentapi.domain.Provider;
import com.siren.sirenpaymentapi.domain.SubscriptionStatus;
import com.siren.sirenpaymentapi.domain.crypto.EncryptedStringConverter;
import com.siren.sirenpaymentapi.domain.entity.BillingKeys;
import com.siren.sirenpaymentapi.domain.entity.PlanPrices;
import com.siren.sirenpaymentapi.domain.entity.Payments;
import com.siren.sirenpaymentapi.domain.entity.Subscriptions;
import com.siren.sirenpaymentapi.dto.payments.PaymentHistoryResponse;
import com.siren.sirenpaymentapi.dto.payments.StuckPayment;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import({QueryDslConfig.class, EncryptedStringConverter.class})
@TestPropertySource(properties = {"payment.crypto.password=test-password", "payment.crypto.salt=abcdef0123456789",
        "spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"})
class PaymentsRepositoryImplTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PaymentsRepository paymentsRepositoryCustom;

    private Subscriptions createSubscription() {
        PlanPrices planPrice = entityManager.persist(
                PlanPrices.builder().plan(Plan.MONTHLY).amount(29000L).status(PlanPriceStatus.ACTIVE).build());
        BillingKeys billingKey = entityManager.persist(
                BillingKeys.builder().userId(1L).provider(Provider.TOSS_PAY).providerCredential("credential")
                        .status(BillingKeyStatus.ACTIVE).build());
        Subscriptions subscription = entityManager.persist(
                Subscriptions.builder().userId(1L).plan(Plan.MONTHLY).amount(29000L)
                        .status(SubscriptionStatus.ACTIVE).billingKey(billingKey).planPrice(planPrice)
                        .nextBillingDate(java.time.LocalDate.now().plusMonths(1))
                        .currentPeriodEnd(LocalDateTime.now().plusMonths(1)).build());
        entityManager.flush();
        return subscription;
    }

    private Payments createPayment(Subscriptions subscription, PaymentStatus status, LocalDateTime createdAtTarget, LocalDateTime attemptedAt) {
        Payments payment = entityManager.persist(
                Payments.builder().orderId("order-" + System.nanoTime()).amount(29000L).status(status)
                        .attemptedAt(attemptedAt).subscription(subscription).build());
        entityManager.flush();
        entityManager.getEntityManager()
                .createQuery("update Payments p set p.createdAt = :createdAt where p.id = :id")
                .setParameter("createdAt", createdAtTarget)
                .setParameter("id", payment.getId())
                .executeUpdate();
        entityManager.clear();
        return payment;
    }

    @Test
    void findStuckInReadyReturnsOldReadyPayments() {
        Subscriptions subscription = createSubscription();
        LocalDateTime now = LocalDateTime.now();
        createPayment(subscription, PaymentStatus.READY, now.minusHours(2), now.minusHours(2));

        List<StuckPayment> result = paymentsRepositoryCustom.findStuckInReady(now.minusHours(1));

        assertEquals(1, result.size());
        assertEquals(subscription.getId(), result.get(0).subscriptionId());
    }

    @Test
    void findStuckInReadyExcludesRecentPayments() {
        Subscriptions subscription = createSubscription();
        LocalDateTime now = LocalDateTime.now();
        createPayment(subscription, PaymentStatus.READY, now, now);

        List<StuckPayment> result = paymentsRepositoryCustom.findStuckInReady(now.minusHours(1));

        assertTrue(result.isEmpty());
    }

    @Test
    void hasNewerAttemptReturnsTrueWhenLaterPaymentExists() {
        Subscriptions subscription = createSubscription();
        LocalDateTime base = LocalDateTime.now().minusHours(1);
        createPayment(subscription, PaymentStatus.DONE, base, base.plusMinutes(30));

        boolean result = paymentsRepositoryCustom.hasNewerAttempt(subscription.getId(), base);

        assertTrue(result);
    }

    @Test
    void hasNewerAttemptReturnsFalseWhenNoLaterPayment() {
        Subscriptions subscription = createSubscription();
        LocalDateTime base = LocalDateTime.now();
        createPayment(subscription, PaymentStatus.DONE, base.minusHours(1), base.minusHours(1));

        boolean result = paymentsRepositoryCustom.hasNewerAttempt(subscription.getId(), base);

        assertFalse(result);
    }

    @Test
    void findByUserIdReturnsPaymentsForThatUserNewestFirst() {
        Subscriptions subscription = createSubscription();
        LocalDateTime now = LocalDateTime.now();
        createPayment(subscription, PaymentStatus.DONE, now.minusDays(1), now.minusDays(1));
        createPayment(subscription, PaymentStatus.DONE, now, now);

        List<PaymentHistoryResponse> result = paymentsRepositoryCustom.findByUserId(1L);

        assertEquals(2, result.size());
        assertTrue(result.get(0).attemptedAt().isAfter(result.get(1).attemptedAt()));
    }

    @Test
    void findByUserIdReturnsEmptyForOtherUser() {
        createSubscription();

        List<PaymentHistoryResponse> result = paymentsRepositoryCustom.findByUserId(999L);

        assertTrue(result.isEmpty());
    }
}
