package com.siren.sirenpaymentapi.dto.kakao.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ReadyRequest(
        @JsonProperty("cid") String cid,
        @JsonProperty("partner_order_id") String partnerOrderId,
        @JsonProperty("partner_user_id") String userId,
        @JsonProperty("item_name") String itemName,
        @JsonProperty("quantity") Integer quantity,
        @JsonProperty("total_amount") Integer totalAmount,
        @JsonProperty("vat_amount") Integer vatAmount,
        @JsonProperty("tax_free_amount") Integer taxFreeAmount,
        @JsonProperty("approval_url") String approvalUrl,
        @JsonProperty("fail_url") String failUrl,
        @JsonProperty("cancel_url") String cancelUrl

) {
}
