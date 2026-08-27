package com.siren.sirenpaymentapi.adaptor;

import com.siren.sirenpaymentapi.dto.kakao.response.ApproveResponse;
import com.siren.sirenpaymentapi.dto.kakao.response.InactiveResponse;
import com.siren.sirenpaymentapi.dto.kakao.response.ReadyResponse;
import com.siren.sirenpaymentapi.dto.kakao.response.SubscriptionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KakaoAdaptorTest {

    private KakaoAdaptor kakaoAdaptor;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://kakao-test");
        mockServer = MockRestServiceServer.bindTo(builder).build();

        kakaoAdaptor = new KakaoAdaptor("http://kakao-test", "secret-key", "cid-1", "http://failure");
        org.springframework.test.util.ReflectionTestUtils.setField(kakaoAdaptor, "restClient", builder.build());
    }

    @Test
    void readyReturnsResponse() {
        mockServer.expect(requestTo("http://kakao-test/online/v1/payment/ready"))
                .andExpect(method(POST))
                .andExpect(header("Authorization", "SECRET_KEY secret-key"))
                .andRespond(withSuccess(
                        "{\"tid\":\"tid-1\",\"next_redirect_pc_url\":\"http://redirect\",\"created_at\":\"2026-08-25\"}",
                        MediaType.APPLICATION_JSON));

        ReadyResponse response = kakaoAdaptor.ready(1L, "order-1", "http://approval");

        assertEquals("tid-1", response.tid());
        assertEquals("http://redirect", response.nextRedirectPcUrl());
    }

    @Test
    void approveReturnsResponse() {
        mockServer.expect(requestTo("http://kakao-test/online/v1/payment/approve"))
                .andExpect(method(POST))
                .andRespond(withSuccess(
                        "{\"sid\":\"sid-1\",\"payment_method_type\":\"MONEY\"}",
                        MediaType.APPLICATION_JSON));

        ApproveResponse response = kakaoAdaptor.approve("tid-1", "order-1", 1L, "pg-token-1");

        assertEquals("sid-1", response.sid());
        assertEquals("MONEY", response.paymentMethodType());
    }

    @Test
    void chargeReturnsResponse() {
        mockServer.expect(requestTo("http://kakao-test/online/v1/payment/subscription"))
                .andExpect(method(POST))
                .andRespond(withSuccess(
                        "{\"sid\":\"sid-1\",\"partner_order_id\":\"order-1\",\"amount\":{\"total\":29000}}",
                        MediaType.APPLICATION_JSON));

        SubscriptionResponse response = kakaoAdaptor.charge("sid-1", 29000L, "order-1", 1L);

        assertEquals("sid-1", response.sid());
        assertEquals(29000, response.amount().total());
    }

    @Test
    void inactiveReturnsResponse() {
        mockServer.expect(requestTo("http://kakao-test/online/v1/payment/manage/subscription/inactive"))
                .andExpect(method(POST))
                .andRespond(withSuccess(
                        "{\"sid\":\"sid-1\",\"status\":\"INACTIVE\"}",
                        MediaType.APPLICATION_JSON));

        InactiveResponse response = kakaoAdaptor.inactive("sid-1");

        assertEquals("INACTIVE", response.status());
    }
}
