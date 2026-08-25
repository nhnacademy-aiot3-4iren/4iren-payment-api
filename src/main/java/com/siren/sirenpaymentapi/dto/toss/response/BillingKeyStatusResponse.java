package com.siren.sirenpaymentapi.dto.toss.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @param code 0
 * @param userId "tutorial-user-001"
 * @param billingKey "example-billingKey"
 * @param status "ACTIVE" | "CREATE" | "FAIL"
 * @param payMethod "CARD"
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BillingKeyStatusResponse(
        int code,
        String userId,
        String billingKey,
        String status,
        String payMethod
) {
}