package com.siren.sirenpaymentapi.dto.toss.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RemoveBillingKeyResponse(
        int code,
        String msg
) {}