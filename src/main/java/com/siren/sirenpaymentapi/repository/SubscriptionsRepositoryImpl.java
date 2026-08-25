package com.siren.sirenpaymentapi.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.siren.sirenpaymentapi.domain.SubscriptionStatus;
import com.siren.sirenpaymentapi.domain.entity.QSubscriptions;
import com.siren.sirenpaymentapi.domain.entity.Subscriptions;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class SubscriptionsRepositoryImpl implements SubscriptionsRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private static final QSubscriptions subscription = QSubscriptions.subscriptions;

    @Override
    public List<Subscriptions> findDueForBilling(LocalDate billingDate) {
        return queryFactory
                .selectFrom(subscription)
                .join(subscription.billingKey).fetchJoin()
                .where(
                        subscription.status.in(SubscriptionStatus.ACTIVE, SubscriptionStatus.PAST_DUE),
                        subscription.nextBillingDate.loe(billingDate)
                )
                .fetch();
    }

    @Override
    public List<Long> findCanceledPastPeriodEnd(LocalDateTime cutoff) {
        return queryFactory
                .select(subscription.id)
                .from(subscription)
                .where(
                        subscription.status.eq(SubscriptionStatus.CANCELED),
                        subscription.currentPeriodEnd.lt(cutoff),
                        subscription.roleDowngradedAt.isNull()
                )
                .fetch();
    }
}
