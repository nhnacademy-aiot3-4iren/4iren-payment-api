package com.siren.sirenpaymentapi.exception;

import com.siren.sirenpaymentapi.domain.Provider;

public class BillingKeyCallbackException extends RuntimeException {
    public BillingKeyCallbackException(Provider provider, String action) {
        super(provider.name() + "빌링키 등록 실패 콜백: action=" + action);
    }
}
