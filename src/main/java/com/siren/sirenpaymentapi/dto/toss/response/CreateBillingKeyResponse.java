package com.siren.sirenpaymentapi.dto.toss.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 *  빌링키 생성 후 응답
 * @param code 응답 코드 성공 시 0
 * @param billingKey 생성된 빌링키
 * @param checkoutUri 사용자를 인증시킬 URI
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CreateBillingKeyResponse(
        int code,
        String billingKey,
        String checkoutUri
) {}