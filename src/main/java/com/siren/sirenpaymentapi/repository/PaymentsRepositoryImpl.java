package com.siren.sirenpaymentapi.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.siren.sirenpaymentapi.domain.PaymentStatus;
import com.siren.sirenpaymentapi.domain.entity.QPayments;
import com.siren.sirenpaymentapi.dto.payments.StuckPayment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class PaymentsRepositoryImpl implements PaymentsRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private static final QPayments payment = QPayments.payments;

    @Override
    public List<StuckPayment> findStuckInReady(LocalDateTime cutoff) {
        return queryFactory
                .select(Projections.constructor(StuckPayment.class,
                        payment.id, payment.subscription.id, payment.attemptedAt))
                .from(payment)
                .where(
                        payment.status.eq(PaymentStatus.READY),
                        payment.createdAt.before(cutoff)
                )
                .fetch();
    }

    @Override
    public boolean hasNewerAttempt(Long subscriptionId, LocalDateTime after) {
        Integer result = queryFactory
                .selectOne()
                .from(payment)
                .where(
                        payment.subscription.id.eq(subscriptionId),
                        payment.attemptedAt.after(after)
                )
                .fetchFirst();
        return result != null;
    }
}
