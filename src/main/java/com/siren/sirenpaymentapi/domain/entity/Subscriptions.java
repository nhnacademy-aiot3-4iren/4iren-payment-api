package com.siren.sirenpaymentapi.domain.entity;

import com.siren.sirenpaymentapi.domain.Plan;
import com.siren.sirenpaymentapi.domain.SubscriptionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscriptions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
public class Subscriptions extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subscription_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "plan", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private Plan plan; // 요금제 (월/연)

    @Column(name = "amount", precision = 10, scale = 0, nullable = false)
    private BigDecimal amount;

    @Column(name = "status", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private SubscriptionStatus status;

    @Column(name = "current_period_end", nullable = false)
    private LocalDateTime currentPeriodEnd; // 현재 주기 종료일 -> 이미 낸 돈으로 이용 가능한 마지막 시점, CANCELED 여도 이때까진 유지

    @Column(name = "next_billing_date", nullable = false)
    private LocalDate nextBillingDate; // 다음 결제 청구일 -> 스케줄러가 이 값을 기준으로 청구대상 조회

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt; // 사용자가 직접 해지를 요청한 시각

    @Column(name = "expired_at")
    private LocalDateTime expiredAt; // 유예기간 소진으로 강제 만료된 시각 -> Account 강등 이벤트 발행 시점과 일치

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billing_key_id", nullable = false, unique = true)
    private BillingKeys billingKey;
}
