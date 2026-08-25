package com.siren.sirenpaymentapi.dto.toss;

import com.siren.sirenpaymentapi.domain.Plan;

/**
 * 등록 시작 시점(startRegistration)에 Redis에 임시로 담아두는 값.
 * 콜백(action: ACTIVATED)이 오면 이 값을 꺼내 billing_keys/subscriptions를 실제로 생성한다.
 * DB엔 아직 아무것도 안 쓴 시점의 값이라 등록 자체가 확정되기 전까지는 Redis TTL로만 존재한다.
 * tokenId도 같은 이유로 미리 저장 - 콜백(토스 서버가 직접 POST) 시점엔 X-TOKEN-ID 헤더가 안 오므로,
 * 등록 시작 시점의 값을 실어뒀다가 role 변경 이벤트 발행 때 꺼내 씀.
 */
public record PendingRegistration(Long userId, Plan plan, Long amount, Long planPriceId, String tokenId) {
}
