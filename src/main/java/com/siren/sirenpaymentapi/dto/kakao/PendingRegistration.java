package com.siren.sirenpaymentapi.dto.kakao;

import com.siren.sirenpaymentapi.domain.Plan;

/**
 * 카카오 등록 1단계(ready)~2단계(approve) 사이 상관관계 값.
 * Toss의 PendingRegistration과 달리 tid도 같이 들고 있어야 한다 - approve 호출에 tid가 필수인데,
 * approval_url 리다이렉트로는 pg_token만 돌아오고 tid는 안 와서(브라우저가 직접 안 들고 있음) 서버가
 * 등록 시작 시점에 미리 저장해뒀다가 콜백 때 꺼내 써야 한다.
 * tokenId도 같은 이유로 미리 저장 - 콜백(PG 리다이렉트) 시점엔 X-TOKEN-ID 헤더 자체가 안 오므로(프론트가
 * 아니라 PG가 직접 부르는 요청), 등록 시작 시점의 값을 여기 실어뒀다가 role 변경 이벤트 발행 때 꺼내 씀.
 */
public record PendingRegistration(Long userId, Plan plan, Long amount, Long planPriceId, String tid, String tokenId) {
}
