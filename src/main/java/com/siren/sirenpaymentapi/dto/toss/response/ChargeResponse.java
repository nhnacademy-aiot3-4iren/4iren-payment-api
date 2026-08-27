package com.siren.sirenpaymentapi.dto.toss.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 자동 결제 승인 응답
 * @param code 0 -> 성공 , -1 -> 실패
 * @param errorCode 실패 시
 * @param msg 실패 시
 * @param transactionId
 * @param payToken 이 결제 건의 고유 값. 환불이나 결제 상태 조회는 일반 결제와 같은 API를 이 값으로 호출함
 * @param approvalTime 승인 날짜
 * @param payMethod "CARD" 인지
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ChargeResponse(
        int code,
        String errorCode,
        String msg,
        String transactionId,
        String payToken,
        String approvalTime,
        String payMethod
) {
}