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
}
