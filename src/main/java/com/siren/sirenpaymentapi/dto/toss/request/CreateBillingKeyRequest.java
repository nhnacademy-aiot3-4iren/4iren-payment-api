package com.siren.sirenpaymentapi.dto.toss.request;

/**
 * curl https://pay.toss.im/api/v1/billing-key \
 * -H "Content-Type: application/json" \
 * -d '{
 *   "apiKey": "sk_test_w5lNQylNqa5lNQe013Nq",
 *   "userId": "tutorial-user-001",
 *   "productDesc": "프리미엄 멤버십 월 구독",
 *   "resultCallback": "https://example-shop.com/api/billing/callback",
 *   "returnSuccessUrl": "https://example-shop.com/billing/success",
 *   "returnFailureUrl": "https://example-shop.com/billing/failure"
 * }'
 */
public record CreateBillingKeyRequest(
        String apiKey, //상점의 API Key. 이후 승인, 조회, 삭제 요청 모두 이 Key와 동일해야함
        String userId, // 가맹점의 사용자 식별 값.
        String productDesc, // 토스 결제창에 표기될 자동 결제 상품명
        String resultCallback, // 사용자가 인증 완료 후 결과를 받을 가맹점 서버 URL, 보안상 HTTPS 권장
        String returnSuccessUrl, // 사용자가 인증 성공 후 이동할 페이지
        String returnFailureUrl // 사용자가 인증 실패 후 이동할 페이지

) {}