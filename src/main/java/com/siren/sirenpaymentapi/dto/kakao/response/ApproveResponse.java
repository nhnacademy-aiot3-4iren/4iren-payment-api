package com.siren.sirenpaymentapi.dto.kakao.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * billing_keys엔 raw_response 같은 원본 보존 컬럼이 없어서(provider_credential+masked_info뿐),
 * 우리가 실제로 쓰는 필드만 남김 - sid(providerCredential이 될 값), payment_method_type(maskedInfo로 씀).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ApproveResponse(
        @JsonProperty("sid") String sid,
        @JsonProperty("payment_method_type") String paymentMethodType
) {
}
