package com.siren.sirenpaymentapi.dto.toss.request;

public record RemoveBillingKeyRequest(
        String apiKey,
        String billingKey
) {}