package com.siren.sirenpaymentapi.dto.kakao.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ApproveRequest(
        @JsonProperty("cid") String cid,
        @JsonProperty("tid") String tid,
        @JsonProperty("partner_order_id") String orderId,
        @JsonProperty("partner_user_id") String userId,
        @JsonProperty("pg_token") String token
) {
}
