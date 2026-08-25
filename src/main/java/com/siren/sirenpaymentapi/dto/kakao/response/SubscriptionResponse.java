package com.siren.sirenpaymentapi.dto.kakao.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 정기결제 2회차 이후 승인 응답 - approve와 사실상 같은 승인 API라 같은 모양으로 옴(공식 문서에서
 * 이 엔드포인트 전용 응답 필드를 확정 못 함, TODO: 실제 호출해서 검증 필요).
 * payments.raw_response에 원본을 통째로 저장할 거라(Toss ChargeResponse와 동일 원칙) 필드를 덜 쳐냄.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SubscriptionResponse(
        @JsonProperty("aid") String aid,
        @JsonProperty("tid") String tid,
        @JsonProperty("cid") String cid,
        @JsonProperty("sid") String sid,
        @JsonProperty("partner_order_id") String orderId,
        @JsonProperty("partner_user_id") String userId,
        @JsonProperty("payment_method_type") String paymentMethodType,
        @JsonProperty("amount") Amount amount,
        @JsonProperty("created_at") String createdAt,
        @JsonProperty("approved_at") String approvedAt
) {
    public record Amount(
            @JsonProperty("total") Integer total,
            @JsonProperty("tax_free") Integer taxFree,
            @JsonProperty("vat") Integer vat,
            @JsonProperty("point") Integer point,
            @JsonProperty("discount") Integer discount,
            @JsonProperty("green_deposit") Integer greenDeposit
    ) {
    }
}
