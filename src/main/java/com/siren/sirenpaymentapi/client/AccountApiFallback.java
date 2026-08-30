package com.siren.sirenpaymentapi.client;

import com.siren.sirenpaymentapi.exception.AccountApiUnavailableException;
import org.springframework.stereotype.Component;

@Component
public class AccountApiFallback implements AccountApiClient {

    @Override
    public String getEmail(Long userId) {
        throw new AccountApiUnavailableException(userId);
    }
}
