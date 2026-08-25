package com.siren.sirenpaymentapi.dto.kakao.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// 정기결제 해지 응답. status가 "INACTIVE"면 성공(공식 문서로 확인함, HTTP 200 + 바디로 옴)
@JsonIgnoreProperties(ignoreUnknown = true)
public record InactiveResponse(
        @JsonProperty("cid") String cid,
        @JsonProperty("sid") String sid,
        @JsonProperty("status") String status,
        @JsonProperty("created_at") String createdAt,
        @JsonProperty("inactivated_at") String inactivatedAt,
        @JsonProperty("last_approved_at") String lastApprovedAt
) {
}
