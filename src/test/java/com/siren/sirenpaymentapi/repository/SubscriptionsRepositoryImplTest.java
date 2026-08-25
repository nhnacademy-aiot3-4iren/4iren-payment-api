package com.siren.sirenpaymentapi.repository;

import com.siren.sirenpaymentapi.config.QueryDslConfig;
import com.siren.sirenpaymentapi.domain.BillingKeyStatus;
import com.siren.sirenpaymentapi.domain.Plan;
import com.siren.sirenpaymentapi.domain.PlanPriceStatus;
import com.siren.sirenpaymentapi.domain.Provider;
import com.siren.sirenpaymentapi.domain.SubscriptionStatus;
import com.siren.sirenpaymentapi.domain.crypto.EncryptedStringConverter;
import com.siren.sirenpaymentapi.domain.entity.BillingKeys;
import com.siren.sirenpaymentapi.domain.entity.PlanPrices;
import com.siren.sirenpaymentapi.domain.entity.Subscriptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import({QueryDslConfig.class, EncryptedStringConverter.class})
@TestPropertySource(properties = {"payment.crypto.password=test-password", "payment.crypto.salt=abcdef0123456789",
        "spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"})
class SubscriptionsRepositoryImplTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private SubscriptionsRepository subscriptionsRepository;

    private Subscriptions createSubscription(SubscriptionStatus status, LocalDate nextBillingDate,
                                              LocalDateTime currentPeriodEnd, LocalDateTime roleDowngradedAt) {
        PlanPrices planPrice = entityManager.persist(
                PlanPrices.builder().plan(Plan.MONTHLY).amount(29000L).status(PlanPriceStatus.ACTIVE).build());
        BillingKeys billingKey = entityManager.persist(
                BillingKeys.builder().userId(1L).provider(Provider.TOSS_PAY).providerCredential("credential")
                        .status(BillingKeyStatus.ACTIVE).build());
        Subscriptions subscription = Subscriptions.builder().userId(1L).plan(Plan.MONTHLY).amount(29000L)
                .status(status).billingKey(billingKey).planPrice(planPrice)
                .nextBillingDate(nextBillingDate).currentPeriodEnd(currentPeriodEnd).build();
        entityManager.persist(subscription);
        if (roleDowngradedAt != null) {
            subscription.markRoleDowngraded();
        }
        entityManager.flush();
        return subscription;
    }

    @Test
    void findDueForBillingReturnsActiveSubscriptionsDueToday() {
        LocalDate today = LocalDate.now();
        createSubscription(SubscriptionStatus.ACTIVE, today, LocalDateTime.now().plusMonths(1), null);

        List<Subscriptions> result = subscriptionsRepository.findDueForBilling(today);

        assertEquals(1, result.size());
    }

    @Test
    void findDueForBillingExcludesFutureBilling() {
        LocalDate today = LocalDate.now();
        createSubscription(SubscriptionStatus.ACTIVE, today.plusDays(1), LocalDateTime.now().plusMonths(1), null);

        List<Subscriptions> result = subscriptionsRepository.findDueForBilling(today);

        assertTrue(result.isEmpty());
    }

    @Test
    void findDueForBillingIncludesPastDue() {
        LocalDate today = LocalDate.now();
        createSubscription(SubscriptionStatus.PAST_DUE, today.minusDays(1), LocalDateTime.now().plusMonths(1), null);

        List<Subscriptions> result = subscriptionsRepository.findDueForBilling(today);

        assertEquals(1, result.size());
    }

    @Test
    void findDueForBillingExcludesCanceled() {
        LocalDate today = LocalDate.now();
        createSubscription(SubscriptionStatus.CANCELED, today, LocalDateTime.now().plusMonths(1), null);

        List<Subscriptions> result = subscriptionsRepository.findDueForBilling(today);

        assertTrue(result.isEmpty());
    }

    @Test
    void findCanceledPastPeriodEndReturnsCanceledPastEndWithoutDowngrade() {
        LocalDateTime now = LocalDateTime.now();
        Subscriptions subscription = createSubscription(SubscriptionStatus.CANCELED, LocalDate.now(), now.minusDays(1), null);

        List<Long> result = subscriptionsRepository.findCanceledPastPeriodEnd(now);

        assertEquals(List.of(subscription.getId()), result);
    }

    @Test
    void findCanceledPastPeriodEndExcludesAlreadyDowngraded() {
        LocalDateTime now = LocalDateTime.now();
        createSubscription(SubscriptionStatus.CANCELED, LocalDate.now(), now.minusDays(1), now);

        List<Long> result = subscriptionsRepository.findCanceledPastPeriodEnd(now);

        assertTrue(result.isEmpty());
    }

    @Test
    void findCanceledPastPeriodEndExcludesFutureEnd() {
        LocalDateTime now = LocalDateTime.now();
        createSubscription(SubscriptionStatus.CANCELED, LocalDate.now(), now.plusDays(1), null);

        List<Long> result = subscriptionsRepository.findCanceledPastPeriodEnd(now);

        assertTrue(result.isEmpty());
    }
}
