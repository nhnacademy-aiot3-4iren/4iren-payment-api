package com.siren.sirenpaymentapi.dto.kakao.request;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 정기결제 2회차 이후 승인 요청(POST /online/v1/payment/subscription).
 * pg_token 없이 사용자 인증 없이 서버 단독으로 sid로 승인한다는 점만 approve와 다름.
 */
public record SubscriptionRequest(
        @JsonProperty("cid") String cid,
        @JsonProperty("sid") String sid,
        @JsonProperty("partner_order_id") String orderId,
        @JsonProperty("partner_user_id") String userId,
        @JsonProperty("item_name") String itemName,
        @JsonProperty("quantity") Integer quantity,
        @JsonProperty("total_amount") Integer totalAmount,
        @JsonProperty("tax_free_amount") Integer taxFreeAmount,
        @JsonProperty("vat_amount") Integer vatAmount
) {
}
