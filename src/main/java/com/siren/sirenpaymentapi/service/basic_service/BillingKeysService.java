package com.siren.sirenpaymentapi.service.basic_service;

import com.siren.sirenpaymentapi.domain.BillingKeyStatus;
import com.siren.sirenpaymentapi.domain.Provider;
import com.siren.sirenpaymentapi.domain.entity.BillingKeys;
import com.siren.sirenpaymentapi.exception.NotFoundBillingKeysException;
import com.siren.sirenpaymentapi.repository.BillingKeysRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BillingKeysService {
    private final BillingKeysRepository billingKeysRepository;

    // 토스 REMOVED 콜백 처리 시 userId로 지금 활성화된 빌링키를 찾기 위해 사용
    public Optional<BillingKeys> findActiveByUserId(Long userId) {
        return billingKeysRepository.findByUserIdAndStatus(userId, BillingKeyStatus.ACTIVE);
    }

    // 결제수단 변경 예약 여부 확인 - 다음 청구 시점에 SubscriptionChargeCoordinator가 사용
    public Optional<BillingKeys> findPendingByUserId(Long userId) {
        return billingKeysRepository.findByUserIdAndStatus(userId, BillingKeyStatus.PENDING);
    }

    /**
     * PG 등록 절차가 확정된 시점에 호출.
     * 등록 확정 전(PG 인증 대기 중)에는 row 자체를 만들지 않으므로.. 이 메서드가 곧 row 생성 시점 = 활성화 시점이다.
     */
    @Transactional
    public BillingKeys registerBillingKeys(Long userId, Provider provider, String credential, String maskedInfo) {
        BillingKeys billingKeys = BillingKeys.builder()
                .userId(userId)
                .provider(provider)
                .providerCredential(credential)
                .maskedInfo(maskedInfo)
                .status(BillingKeyStatus.ACTIVE)
                .build();
        return billingKeysRepository.save(billingKeys);
    }

    /**
     * 결제수단 변경 시 기존 빌링키를 해지할 때 호출.
     * row를 삭제하지 않고 status만 DELETED로 전이(이력 보존).
     */
    @Transactional
    public void deleteBillingKeys(Long billingKeyId) {
        BillingKeys billingKeys = billingKeysRepository.findById(billingKeyId)
                .orElseThrow(()-> new NotFoundBillingKeysException(billingKeyId));
        billingKeys.markDeleted();
    }

    /**
     * 결제수단 변경 시작 시 호출 - 새 빌링키를 PENDING으로만 저장한다.
     * 기존 ACTIVE 키/구독은 여기서 전혀 안 건드림 - 실제 교체는 다음 청구 시점에
     * BillingKeyRegistrationService.applyPendingBillingKeyIfAny가 담당.
     * 이미 예약된 게 있으면(변경을 다시 시작한 경우) 마지막 요청만 유효하도록 먼저 폐기한다.
     */
    @Transactional
    public BillingKeys registerPendingBillingKey(Long userId, Provider provider, String credential, String maskedInfo) {
        findPendingByUserId(userId).ifPresent(BillingKeys::markDeleted);

        BillingKeys billingKeys = BillingKeys.builder()
                .userId(userId)
                .provider(provider)
                .providerCredential(credential)
                .maskedInfo(maskedInfo)
                .status(BillingKeyStatus.PENDING)
                .build();
        return billingKeysRepository.save(billingKeys);
    }

    // 예약된 빌링키를 실제로 활성화 - 다음 청구 시점에 호출
    @Transactional
    public void activateBillingKey(Long billingKeyId) {
        BillingKeys billingKeys = billingKeysRepository.findById(billingKeyId)
                .orElseThrow(() -> new NotFoundBillingKeysException(billingKeyId));
        billingKeys.markActive();
    }
}
