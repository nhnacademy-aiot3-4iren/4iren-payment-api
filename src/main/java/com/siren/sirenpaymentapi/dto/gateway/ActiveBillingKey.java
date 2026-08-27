package com.siren.sirenpaymentapi.dto.gateway;

import com.siren.sirenpaymentapi.domain.Provider;

/**
 * 실제로 이번 청구에 사용할 provider/credential.
 * BillingKeyRegistrationService.applyPendingBillingKeyIfAny가 예약된 결제수단 변경이 있으면
 * 새 키로 스왑한 뒤 반환하고, 없으면 원래 넘어온 값을 그대로 반환한다.
 */
public record ActiveBillingKey(Provider provider, String providerCredential) {
}
