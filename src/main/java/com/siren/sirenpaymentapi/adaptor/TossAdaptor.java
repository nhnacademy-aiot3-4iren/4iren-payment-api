package com.siren.sirenpaymentapi.adaptor;

import com.siren.sirenpaymentapi.dto.toss.request.BillingKeyStatusRequest;
import com.siren.sirenpaymentapi.dto.toss.request.ChargeRequest;
import com.siren.sirenpaymentapi.dto.toss.request.CreateBillingKeyRequest;
import com.siren.sirenpaymentapi.dto.toss.request.RemoveBillingKeyRequest;
import com.siren.sirenpaymentapi.dto.toss.response.BillingKeyStatusResponse;
import com.siren.sirenpaymentapi.dto.toss.response.ChargeResponse;
import com.siren.sirenpaymentapi.dto.toss.response.CreateBillingKeyResponse;
import com.siren.sirenpaymentapi.dto.toss.response.RemoveBillingKeyResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class TossAdaptor {
    private static final String PRODUCT_DESC = "4iren 정기 구독 자동결제"; // 인터페이스가 플랜별 설명을 안 받아서 고정값 사용

    private final RestClient restClient;
    private final String apiKey;
    private final String successUrl;
    private final String failureUrl;

    public TossAdaptor(@Value("${toss.base-url}") String baseUrl,
                       @Value("${toss.secret-key}") String apiKey,
                       @Value("${payment.successUrl}")  String successUrl,
                       @Value("${payment.failureUrl}")  String failureUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.apiKey = apiKey;
        this.successUrl = successUrl;
        this.failureUrl = failureUrl;
    }

    // 빌링 키 생성하기
    public CreateBillingKeyResponse createBillingKey(Long userId, String returnUrl) {
        return post(
                "/api/v1/billing-key",
                new CreateBillingKeyRequest(apiKey, String.valueOf(userId), PRODUCT_DESC, returnUrl, successUrl, failureUrl),
                CreateBillingKeyResponse.class);
    }

    /**
     * 빌링 키 상태 확인
     * 사용자가 토스 앱에서 결제수단을 바꾸면 payMethod가 업데이트될 수 있으니
     * 회원 관리 화면에 결제수단을 보여준다면 이 API로 최신 값을 확인
     */
    public BillingKeyStatusResponse getBillingKeyStatus(String userId) {
        return post(
                "/api/v1/billing-key/status",
                new BillingKeyStatusRequest(apiKey, userId),
                BillingKeyStatusResponse.class);
    }

    /**
     * 자동결제 승인 API
     */
    public ChargeResponse executeBilling(String billingKey,  Long amount, String orderId){
        ChargeRequest request = new ChargeRequest(
                apiKey, billingKey, orderId, PRODUCT_DESC, amount, 0, 0, false, "GENERAL", true);
        return post("/api/v1/billing-key/bill", request, ChargeResponse.class);
    }

    /**
     * 빌링 키 관리하기 - 빌링 키 삭제하기
     */
    public RemoveBillingKeyResponse removeBillingKey(String billingKey){
        return post(
                "/api/v1/billing-key/remove",
                new RemoveBillingKeyRequest(apiKey, billingKey),
                RemoveBillingKeyResponse.class);
    }


    private <T> T post(String uri, Object body, Class<T> responseType) {
        return restClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(responseType);
    }
}
