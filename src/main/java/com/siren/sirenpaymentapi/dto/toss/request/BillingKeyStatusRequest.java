package com.siren.sirenpaymentapi.dto.toss.request;

/**
 * 빌링 키 활성상태 체크 요청 DTO
 */
public record BillingKeyStatusRequest(
        String apiKey,
        String userId
) {}