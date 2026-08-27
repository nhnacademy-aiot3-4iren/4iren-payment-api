package com.siren.sirenpaymentapi.adaptor;

import com.siren.sirenpaymentapi.dto.kakao.request.ApproveRequest;
import com.siren.sirenpaymentapi.dto.kakao.request.InactiveRequest;
import com.siren.sirenpaymentapi.dto.kakao.request.ReadyRequest;
import com.siren.sirenpaymentapi.dto.kakao.request.SubscriptionRequest;
import com.siren.sirenpaymentapi.dto.kakao.response.ApproveResponse;
import com.siren.sirenpaymentapi.dto.kakao.response.InactiveResponse;
import com.siren.sirenpaymentapi.dto.kakao.response.ReadyResponse;
import com.siren.sirenpaymentapi.dto.kakao.response.SubscriptionResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 카카오페이 정기결제 API 클라이언트
 */
@Component
public class KakaoAdaptor {
    private static final String ITEM_NAME = "4iren 정기 구독 자동결제";

    private final RestClient restClient;
    private final String secretKey;
    private final String cid;
    private final String failureUrl;

    public KakaoAdaptor(@Value("${kakao.base-url}") String baseUrl,
                         @Value("${kakao.secret-key}") String secretKey,
                         @Value("${kakao.cid}") String cid,
                         @Value("${payment.failureUrl}") String failureUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.secretKey = secretKey;
        this.cid = cid;
        this.failureUrl = failureUrl;
    }

    /**
     * 정기결제 SID 발급 1단계 - 단건결제와 동일 API, total_amount=0으로 승인 없이 SID만 받기 위해 호출.
     */
    public ReadyResponse ready(Long userId, String orderId, String approvalUrl) {
        ReadyRequest request = new ReadyRequest(cid, orderId, String.valueOf(userId), ITEM_NAME,
                1, 0, 0, 0, approvalUrl, failureUrl, failureUrl);
        return post("/online/v1/payment/ready", request, ReadyResponse.class);
    }

    /**
     * 정기결제 SID 발급 2단계 - 사용자 인증 완료 후 pg_token으로 승인, 응답에 sid 포함.
     */
    public ApproveResponse approve(String tid, String orderId, Long userId, String pgToken) {
        ApproveRequest request = new ApproveRequest(cid, tid, orderId, String.valueOf(userId), pgToken);
        return post("/online/v1/payment/approve", request, ApproveResponse.class);
    }

    /**
     * 2회차 이후 정기결제 승인 - sid로 사용자 개입 없이 서버 단독 호출.
     */
    public SubscriptionResponse charge(String sid, Long amount, String orderId, Long userId) {
        SubscriptionRequest request = new SubscriptionRequest(cid, sid, orderId, String.valueOf(userId), ITEM_NAME,
                1, Math.toIntExact(amount), 0, 0);
        return post("/online/v1/payment/subscription", request, SubscriptionResponse.class);
    }

    /**
     * 정기결제 해지 - sid 비활성화.
     */
    public InactiveResponse inactive(String sid) {
        return post("/online/v1/payment/manage/subscription/inactive",
                new InactiveRequest(cid, sid), InactiveResponse.class);
    }

    private <T> T post(String uri, Object body, Class<T> responseType) {
        return restClient.post()
                .uri(uri)
                .header("Authorization", "SECRET_KEY " + secretKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(responseType);
    }
}
