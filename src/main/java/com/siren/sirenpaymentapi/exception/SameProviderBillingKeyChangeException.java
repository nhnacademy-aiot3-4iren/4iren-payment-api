package com.siren.sirenpaymentapi.exception;

import com.siren.sirenpaymentapi.domain.Provider;

// 409 - PG(Toss/Kakao)는 같은 userId로 이미 활성인 빌링키가 있으면 새 빌링키 발급 자체를 거부함(BILLING_KEY_ALREADY_ACTIVATED).
// 그래서 결제수단 변경은 지금 활성 provider와 다른 provider로만 허용한다(Toss<->Kakao만 가능, Toss->Toss 불가).
public class SameProviderBillingKeyChangeException extends RuntimeException {
    public SameProviderBillingKeyChangeException(Provider provider) {
        super(provider + "는 이미 활성 상태입니다. 결제수단 변경은 다른 PG로만 가능합니다.");
    }
}
