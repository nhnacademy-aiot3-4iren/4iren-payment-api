package com.siren.sirenpaymentapi.client;

import com.siren.sirenpaymentapi.exception.AccountApiUnavailableException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class AccountApiFallbackTest {

    private final AccountApiFallback fallback = new AccountApiFallback();

    @Test
    void getEmailThrowsAccountApiUnavailableException() {
        assertThrows(AccountApiUnavailableException.class, () -> fallback.getEmail(1L));
    }
}
