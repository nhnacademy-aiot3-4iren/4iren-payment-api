package com.siren.sirenpaymentapi.dto.toss.request;

/**
 * 자동 결제 승인 요청 DTO
 * @param apiKey 빌링키 생성 때 사용한 API Key와 동일해야함
 * @param billingKey adaptor.getBillingKeyStatus 로 확인한 빌링 키
 * @param orderNo 주문번호 unique (_,-,:,.,^,@,=) 만 허용
 * @param productDesc 상품 설명
 * @param amount 결제 요청 금액
 * @param spreadOut 카드 할부 개월 수 0이 일시불, 5만원 미만은 일시불만 가능
 * @param amountTaxFree 결제 금액 중 비과세 금액. 없으면 0
 * @param cashReceipt 토스 형금 영수증 자동발행 사용 여부
 * @param cashReceiptTradeOption 현금영수증 발행 유형 GENERAL(일반) | CULTURE(문화비) | PUBLIC_TP (대중교통) 중 선택
 * @param sendFailPush 결제 실패 시 사용자에게 실패 알림을 보낼 지 여부
 */
public record ChargeRequest(
        String apiKey,
        String billingKey,
        String orderNo,
        String productDesc,
        Long amount, // 결제 가격
        int spreadOut,
        int amountTaxFree, //세금 가격
        boolean cashReceipt,
        String cashReceiptTradeOption,
        boolean sendFailPush
) { }