package com.siren.sirenpaymentapi.dto.kakao.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 카카오페이 API 실패 응답(HTTP 4xx 바디) - 성공 응답과 달리 필드가 error_code/error_message뿐이라
 * 성공 응답 DTO(SubscriptionResponse 등)와 별도로 둠. 근거: "카카오페이 결제 오류코드" 문서(사용자 제공).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoErrorResponse(
        @JsonProperty("error_code") Integer errorCode,
        @JsonProperty("error_message") String errorMessage
) {
}
