package com.siren.sirenpaymentapi.adaptor;

import com.siren.sirenpaymentapi.dto.toss.response.BillingKeyStatusResponse;
import com.siren.sirenpaymentapi.dto.toss.response.ChargeResponse;
import com.siren.sirenpaymentapi.dto.toss.response.CreateBillingKeyResponse;
import com.siren.sirenpaymentapi.dto.toss.response.RemoveBillingKeyResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.POST;

class TossAdaptorTest {

    private TossAdaptor tossAdaptor;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://toss-test");
        mockServer = MockRestServiceServer.bindTo(builder).build();

        tossAdaptor = new TossAdaptor("http://toss-test", "test-key", "http://success", "http://failure");
        replaceRestClient(builder.build());
    }

    private void replaceRestClient(RestClient restClient) {
        org.springframework.test.util.ReflectionTestUtils.setField(tossAdaptor, "restClient", restClient);
    }

    @Test
    void createBillingKeyReturnsResponse() {
        mockServer.expect(requestTo("http://toss-test/api/v1/billing-key"))
                .andExpect(method(POST))
                .andRespond(withSuccess(
                        "{\"code\":0,\"billingKey\":\"billing-key-1\",\"checkoutUri\":\"http://checkout\"}",
                        MediaType.APPLICATION_JSON));

        CreateBillingKeyResponse response = tossAdaptor.createBillingKey(1L, "http://return-url");

        assertEquals(0, response.code());
        assertEquals("billing-key-1", response.billingKey());
        assertEquals("http://checkout", response.checkoutUri());
    }

    @Test
    void getBillingKeyStatusReturnsResponse() {
        mockServer.expect(requestTo("http://toss-test/api/v1/billing-key/status"))
                .andExpect(method(POST))
                .andRespond(withSuccess(
                        "{\"code\":0,\"userId\":\"1\",\"billingKey\":\"billing-key-1\",\"status\":\"ACTIVE\",\"payMethod\":\"CARD\"}",
                        MediaType.APPLICATION_JSON));

        BillingKeyStatusResponse response = tossAdaptor.getBillingKeyStatus("1");

        assertEquals("ACTIVE", response.status());
        assertEquals("CARD", response.payMethod());
    }

    @Test
    void executeBillingReturnsResponse() {
        mockServer.expect(requestTo("http://toss-test/api/v1/billing-key/bill"))
                .andExpect(method(POST))
                .andRespond(withSuccess(
                        "{\"code\":0,\"transactionId\":\"tx-1\",\"payToken\":\"pay-token-1\",\"approvalTime\":\"2026-08-25\",\"payMethod\":\"CARD\"}",
                        MediaType.APPLICATION_JSON));

        ChargeResponse response = tossAdaptor.executeBilling("billing-key-1", 29000L, "order-1");

        assertEquals(0, response.code());
        assertEquals("tx-1", response.transactionId());
        assertEquals("pay-token-1", response.payToken());
    }

    @Test
    void removeBillingKeyReturnsResponse() {
        mockServer.expect(requestTo("http://toss-test/api/v1/billing-key/remove"))
                .andExpect(method(POST))
                .andRespond(withSuccess("{\"code\":0,\"msg\":\"success\"}", MediaType.APPLICATION_JSON));

        RemoveBillingKeyResponse response = tossAdaptor.removeBillingKey("billing-key-1");

        assertEquals(0, response.code());
        assertEquals("success", response.msg());
    }
}
