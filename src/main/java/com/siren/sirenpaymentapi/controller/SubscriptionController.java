package com.siren.sirenpaymentapi.controller;

import com.siren.sirenpaymentapi.dto.subscriptions.SubscriptionResponse;
import com.siren.sirenpaymentapi.security.RequireRole;
import com.siren.sirenpaymentapi.security.Role;
import com.siren.sirenpaymentapi.service.BillingKeyRegistrationService;
import com.siren.sirenpaymentapi.service.basic_service.SubscriptionsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * PG 무관 구독 관리 엔드포인트 - 등록/콜백은 PG별 컨트롤러(controller/toss, controller/kakao)가 담당하지만
 */
@RestController
@RequestMapping("/api/payment/subscriptions")
@RequiredArgsConstructor
@RequireRole(value = Role.OWNER)
public class SubscriptionController {
    private final BillingKeyRegistrationService billingKeyRegistrationService;
    private final SubscriptionsService subscriptionsService;

    @GetMapping
    public ResponseEntity<SubscriptionResponse> getCurrentSubscription(@RequestHeader("X-USER-ID") Long userId) {
        return ResponseEntity.ok(subscriptionsService.findLatestByUserId(userId));
    }

    @DeleteMapping
    public ResponseEntity<Void> cancelSubscription(@RequestHeader("X-USER-ID") Long userId) {
        billingKeyRegistrationService.cancelSubscription(userId);
        return ResponseEntity.noContent().build();
    }
}
