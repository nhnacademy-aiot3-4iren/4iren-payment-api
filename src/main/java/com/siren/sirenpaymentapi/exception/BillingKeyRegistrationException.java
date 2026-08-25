package com.siren.sirenpaymentapi.exception;

import com.siren.sirenpaymentapi.domain.Provider;

public class BillingKeyRegistrationException extends RuntimeException {
    public BillingKeyRegistrationException(Provider provider) {
        super(provider.name() + "빌링키 생성 실패");
    }
}
