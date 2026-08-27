package com.siren.sirenpaymentapi.domain.entity;

import com.siren.sirenpaymentapi.domain.BillingKeyStatus;
import com.siren.sirenpaymentapi.domain.Provider;
import com.siren.sirenpaymentapi.domain.crypto.EncryptedStringConverter;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "billing_keys")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Builder
public class BillingKeys extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "billing_key_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "provider", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private Provider provider; // Toss, kakao , naver

    @Column(name = "provider_credential", columnDefinition = "TEXT", nullable = false)
    @Convert(converter = EncryptedStringConverter.class)
    private String providerCredential; // 평문으로 다루면 됨 - DB 입출력 시 컨버터가 자동 암복호화
    // 이 값만 있으면 그 사람 결제수단으로 청구 할 수 있음 (구독자의 카드를 긁을 수 있는 열쇠)

    @Column(name = "masked_info", length = 100)
    private String maskedInfo;

    @Column(name = "status", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private BillingKeyStatus status;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    //빌링 키 소프트 삭제
    public void markDeleted() {
        this.deletedAt = LocalDateTime.now(ZONE_ID);
        this.status = BillingKeyStatus.DELETED;
    }

    // 예약(PENDING)된 결제수단 변경을 다음 청구 시점에 실제로 활성화할 때 호출
    public void markActive() {
        this.status = BillingKeyStatus.ACTIVE;
    }

}
