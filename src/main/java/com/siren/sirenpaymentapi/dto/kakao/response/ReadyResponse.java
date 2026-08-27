package com.siren.sirenpaymentapi.dto.kakao.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ReadyResponse(
        @JsonProperty("tid") String tid,
        @JsonProperty("next_redirect_pc_url") String nextRedirectPcUrl,
        @JsonProperty("created_at") String createdAt
) {
}
