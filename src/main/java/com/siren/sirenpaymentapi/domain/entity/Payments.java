package com.siren.sirenpaymentapi.domain.entity;

import com.siren.sirenpaymentapi.domain.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
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
    private String providerTransactionId; // TOSS - transactionId(승인 1건의 개별 거래 식별자), kakao - tid, naver - paymentID -> 성공 시에만 존재

    @Column(name = "pay_token", length = 100)
    private String payToken; // TOSS - payToken(결제 건 전체 식별자, 환불/상태조회 API가 요구함) -> 성공 시에만 존재. 카카오/네이버는 tid/paymentID 자체가 이 역할을 겸해서 별도 컬럼 불필요할 수 있음(연동 시 재확인)

    @Column(name = "amount", nullable = false)
    private Long amount;

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

    // 성공시 (결제 후)
    public void markSucceeded(String providerTransactionId, String payToken, String rawResponse) {
        this.status = PaymentStatus.DONE;
        this.providerTransactionId = providerTransactionId;
        this.payToken = payToken;
        this.rawResponse = rawResponse;
        this.approvedAt = LocalDateTime.now(ZONE_ID);
    }

    // 결제 실패 시 (실패 사유, 실제 사유 원본)
    public void markFailed(String failureReason, String rawResponse) {
        this.status = PaymentStatus.FAILED;
        this.failureReason = failureReason;
        this.rawResponse = rawResponse;
    }
}
