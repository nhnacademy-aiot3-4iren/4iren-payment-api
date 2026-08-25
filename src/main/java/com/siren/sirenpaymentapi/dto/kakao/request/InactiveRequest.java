package com.siren.sirenpaymentapi.dto.kakao.request;

import com.fasterxml.jackson.annotation.JsonProperty;

// 정기결제 해지(POST /online/v1/payment/manage/subscription/inactive)
public record InactiveRequest(
        @JsonProperty("cid") String cid,
        @JsonProperty("sid") String sid
) {
}
