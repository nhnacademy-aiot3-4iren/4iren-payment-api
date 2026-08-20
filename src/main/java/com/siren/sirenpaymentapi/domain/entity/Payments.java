package com.siren.sirenpaymentapi.domain.entity;

import com.siren.sirenpaymentapi.domain.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments", uniqueConstraints = {
        @UniqueConstraint(name = "unique_order_id", columnNames = "order_id")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Builder
public class Payments extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long id;

    @Column(name = "order_id", length = 64, nullable = false, unique = true)
    private String orderId; // 멱등성 키로도 사용

    @Column(name = "provider_transaction_id", length = 100)
    private String providerTransactionId; // TOSS - PaymentKey, kakao - tid, naver- paymentID -> 성공 시에만 존재

    @Column(name = "amount", precision = 10, scale = 0, nullable = false)
    private BigDecimal amount;

    @Column(name = "status", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @Column(name = "failure_reason")
    private String failureReason; // 카드사/PG 응답 메시지 요약

    @Column(name = "raw_response", columnDefinition = "JSON")
    private String rawResponse; // 분쟁 / cs 대응용 원본 그대로

    @Column(name = "attempted_at", nullable = false)
    private LocalDateTime attemptedAt; //승인 시도 일시

    @Column(name = "approved_at")
    private LocalDateTime approvedAt; // 승인 완료 일시

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscriptions subscription;

}
