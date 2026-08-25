package com.siren.sirenpaymentapi.domain.entity;

import com.siren.sirenpaymentapi.domain.Plan;
import com.siren.sirenpaymentapi.domain.PlanPriceStatus;
import jakarta.persistence.*;
import lombok.*;

/**
 * Plan별 현재 가격 + 이력. billing_keys가 결제수단 변경 시 쓰는 패턴과 동일 -
 * 가격이 바뀌면 새 row를 ACTIVE로 추가하고 기존 row는 INACTIVE로 전환(UPDATE로 덮어쓰지 않음).
 * "지금 가격"은 findByPlanAndStatus(plan, ACTIVE) 하나로 조회 - 정렬/시간 비교 없음.
 */
@Entity
@Table(name = "plan_prices")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Builder
public class PlanPrices extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "plan_price_id")
    private Long id;

    @Column(name = "plan", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private Plan plan;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "status", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private PlanPriceStatus status;

    // 새 가격이 등록되면서 밀려날 때 호출
    public void markInactive() {
        this.status = PlanPriceStatus.INACTIVE;
    }
}
