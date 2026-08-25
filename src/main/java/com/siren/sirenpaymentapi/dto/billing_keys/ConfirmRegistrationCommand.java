package com.siren.sirenpaymentapi.dto.billing_keys;

import com.siren.sirenpaymentapi.domain.Plan;
import com.siren.sirenpaymentapi.domain.Provider;

public record ConfirmRegistrationCommand(
        Long userId,
        Provider provider,
        String providerCredential,
        String maskedInfo,
        Plan plan,
        Long amount,
        Long planPriceId,
        String tokenId
) {
}
