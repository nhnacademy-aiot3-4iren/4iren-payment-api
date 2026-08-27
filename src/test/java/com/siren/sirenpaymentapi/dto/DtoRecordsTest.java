package com.siren.sirenpaymentapi.dto;

import com.siren.sirenpaymentapi.domain.Plan;
import com.siren.sirenpaymentapi.dto.billing_keys.StartRegistrationRequest;
import com.siren.sirenpaymentapi.dto.billing_keys.StartRegistrationResponse;
import com.siren.sirenpaymentapi.dto.kakao.request.ApproveRequest;
import com.siren.sirenpaymentapi.dto.kakao.request.InactiveRequest;
import com.siren.sirenpaymentapi.dto.kakao.request.ReadyRequest;
import com.siren.sirenpaymentapi.dto.kakao.request.SubscriptionRequest;
import com.siren.sirenpaymentapi.dto.kakao.response.SubscriptionResponse;
import com.siren.sirenpaymentapi.dto.toss.request.BillingKeyStatusRequest;
import com.siren.sirenpaymentapi.dto.toss.request.ChargeRequest;
import com.siren.sirenpaymentapi.dto.toss.request.CreateBillingKeyRequest;
import com.siren.sirenpaymentapi.dto.toss.request.RemoveBillingKeyRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DtoRecordsTest {

    @Test
    void readyRequestHoldsFields() {
        ReadyRequest request = new ReadyRequest("cid-1", "order-1", "1", "item", 1, 0, 0, 0,
                "http://approval", "http://fail", "http://cancel");

        assertEquals("cid-1", request.cid());
        assertEquals("order-1", request.partnerOrderId());
        assertEquals("1", request.userId());
        assertEquals("item", request.itemName());
        assertEquals(1, request.quantity());
        assertEquals("http://approval", request.approvalUrl());
        assertEquals("http://fail", request.failUrl());
        assertEquals("http://cancel", request.cancelUrl());
    }

    @Test
    void chargeRequestHoldsFields() {
        ChargeRequest request = new ChargeRequest("key-1", "billing-key-1", "order-1", "desc", 29000L,
                0, 0, false, "GENERAL", true);

        assertEquals("key-1", request.apiKey());
        assertEquals("billing-key-1", request.billingKey());
        assertEquals("order-1", request.orderNo());
        assertEquals(29000L, request.amount());
        assertEquals("GENERAL", request.cashReceiptTradeOption());
        assertTrue(request.sendFailPush());
    }

    @Test
    void subscriptionRequestHoldsFields() {
        SubscriptionRequest request = new SubscriptionRequest("cid-1", "sid-1", "order-1", "1", "item",
                1, 29000, 0, 0);

        assertEquals("cid-1", request.cid());
        assertEquals("sid-1", request.sid());
        assertEquals("order-1", request.orderId());
        assertEquals(29000, request.totalAmount());
    }

    @Test
    void createBillingKeyRequestHoldsFields() {
        CreateBillingKeyRequest request = new CreateBillingKeyRequest("key-1", "1", "desc",
                "http://callback", "http://success", "http://failure");

        assertEquals("key-1", request.apiKey());
        assertEquals("1", request.userId());
        assertEquals("http://callback", request.resultCallback());
        assertEquals("http://success", request.returnSuccessUrl());
        assertEquals("http://failure", request.returnFailureUrl());
    }

    @Test
    void approveRequestHoldsFields() {
        ApproveRequest request = new ApproveRequest("cid-1", "tid-1", "order-1", "1", "pg-token-1");

        assertEquals("cid-1", request.cid());
        assertEquals("tid-1", request.tid());
        assertEquals("pg-token-1", request.token());
    }

    @Test
    void removeBillingKeyRequestHoldsFields() {
        RemoveBillingKeyRequest request = new RemoveBillingKeyRequest("key-1", "billing-key-1");

        assertEquals("key-1", request.apiKey());
        assertEquals("billing-key-1", request.billingKey());
    }

    @Test
    void billingKeyStatusRequestHoldsFields() {
        BillingKeyStatusRequest request = new BillingKeyStatusRequest("key-1", "1");

        assertEquals("key-1", request.apiKey());
        assertEquals("1", request.userId());
    }

    @Test
    void inactiveRequestHoldsFields() {
        InactiveRequest request = new InactiveRequest("cid-1", "sid-1");

        assertEquals("cid-1", request.cid());
        assertEquals("sid-1", request.sid());
    }

    @Test
    void startRegistrationRequestHoldsFields() {
        StartRegistrationRequest request = new StartRegistrationRequest(Plan.MONTHLY);

        assertEquals(Plan.MONTHLY, request.plan());
    }

    @Test
    void startRegistrationResponseHoldsFields() {
        StartRegistrationResponse response = new StartRegistrationResponse("http://redirect-url");

        assertEquals("http://redirect-url", response.redirectUrl());
    }

    @Test
    void subscriptionResponseAmountHoldsFields() {
        SubscriptionResponse.Amount amount = new SubscriptionResponse.Amount(29000, 0, 2636, 0, 0, 0);

        assertEquals(29000, amount.total());
        assertEquals(2636, amount.vat());
    }
}
