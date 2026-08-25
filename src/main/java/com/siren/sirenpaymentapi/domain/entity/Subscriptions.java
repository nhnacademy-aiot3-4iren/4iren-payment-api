package com.siren.sirenpaymentapi.domain.entity;

import com.siren.sirenpaymentapi.domain.Plan;
import com.siren.sirenpaymentapi.domain.SubscriptionStatus;
import jakarta.persistence.*;
import lombok.*;

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

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "status", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private SubscriptionStatus status;

    @Column(name = "current_period_end", nullable = false)
    private LocalDateTime currentPeriodEnd; // 현재 주기 종료일 -> 이미 낸 돈으로 이용 가능한 마지막 시점, CANCELED 여도 이때까진 유지

    @Column(name = "next_billing_date", nullable = false)
    private LocalDate nextBillingDate; // 다음 결제 청구일 -> 스케줄러가 이 값을 기준으로 청구대상 조회

    @Builder.Default
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt; // 사용자가 직접 해지를 요청한 시각

    @Column(name = "expired_at")
    private LocalDateTime expiredAt; // 유예기간 소진으로 강제 만료된 시각 -> Account 강등 이벤트 발행 시점과 일치

    @Column(name = "role_downgraded_at")
    private LocalDateTime roleDowngradedAt; // 해지(CANCELED) 후 계약기간이 끝나 실제로 OWNER 권한을 잃은 시각.
    // status는 계속 CANCELED로 유지(EXPIRED로 안 바꿈 - "결제 실패로 잘림"과 "본인이 해지함"은 다른 사유라서
    // status 하나만 보고 구분 가능해야 함). 이 컬럼이 null->값 있음으로 바뀌는 게 CanceledSubscriptionExpiryScheduler가
    // 같은 row를 다음날 또 처리하지 않게 막는 표시 역할도 겸함.

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billing_key_id", nullable = false, unique = true)
    private BillingKeys billingKey;

    // 가입 시점 가격 고정(grandfathering) - 나중에 가격이 바뀌어도 이 값은 안 바뀜, amount는 그 시점 스냅샷.
    // CS 추적용 참조라 이후 절대 재대입 안 함(billingKey처럼 바뀌는 값이 아님).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_price_id", nullable = false)
    private PlanPrices planPrice;

    // 사용자가 결제수단을 바꿀 경우 (구독 이력도 유지해야 함)
    public void replaceBillingKey(BillingKeys newKey) {
        this.billingKey = newKey;
    }

    // 정상 주기 갱신
    public void advanceBillingCycle(){
        this.currentPeriodEnd = this.plan == Plan.MONTHLY
                ? this.currentPeriodEnd.plusMonths(1)
                : this.currentPeriodEnd.plusYears(1);
        this.nextBillingDate = this.currentPeriodEnd.toLocalDate();
    }
    
    // PAST_DUE -> ACTIVE로 다시 복귀
    public void recoverActive() {
        this.status = SubscriptionStatus.ACTIVE;
        this.retryCount = 0;
        advanceBillingCycle();
    }

    // 유예기간 - 매일 재시도 원칙이라 다음 재시도일은 항상 내일로 잡는다(최대 재시도 횟수 판단은 SubscriptionsService)
    public void markPastDue() {
        this.status = SubscriptionStatus.PAST_DUE;
        this.retryCount += 1;
        this.nextBillingDate = LocalDate.now(ZONE_ID).plusDays(1);
    }

    // 만료
    public void markExpired() {
        this.status = SubscriptionStatus.EXPIRED;
        this.expiredAt = LocalDateTime.now(ZONE_ID);
    }
    // 구독 취소
    public void markCanceled() {
        this.status = SubscriptionStatus.CANCELED;
        this.canceledAt = LocalDateTime.now(ZONE_ID);
    }

    // 해지 후 계약기간(currentPeriodEnd)이 실제로 끝났을 때 호출 - status는 CANCELED 그대로 두고
    // (EXPIRED와 다른 사유라서) 실제로 권한을 잃은 시각만 기록한다.
    public void markRoleDowngraded() {
        this.roleDowngradedAt = LocalDateTime.now(ZONE_ID);
    }



}
