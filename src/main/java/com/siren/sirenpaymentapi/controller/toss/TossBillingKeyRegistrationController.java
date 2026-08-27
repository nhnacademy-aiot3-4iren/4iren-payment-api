package com.siren.sirenpaymentapi.controller.toss;

import com.siren.sirenpaymentapi.domain.Provider;
import com.siren.sirenpaymentapi.domain.RegistrationMode;
import com.siren.sirenpaymentapi.domain.entity.PlanPrices;
import com.siren.sirenpaymentapi.dto.billing_keys.ConfirmRegistrationCommand;
import com.siren.sirenpaymentapi.dto.billing_keys.StartRegistrationRequest;
import com.siren.sirenpaymentapi.dto.billing_keys.StartRegistrationResponse;
import com.siren.sirenpaymentapi.dto.gateway.ConfirmedBillingKey;
import com.siren.sirenpaymentapi.dto.gateway.RegistrationStart;
import com.siren.sirenpaymentapi.dto.toss.PendingRegistration;
import com.siren.sirenpaymentapi.gateway.RecurringPaymentGatewayRegistry;
import com.siren.sirenpaymentapi.service.BillingKeyRegistrationService;
import com.siren.sirenpaymentapi.service.basic_service.BillingKeysService;
import com.siren.sirenpaymentapi.service.basic_service.PlanPricesService;
import com.siren.sirenpaymentapi.service.cache.TossPendingRegistrationCache;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * 토스페이 빌링키 등록 흐름 전용 컨트롤러 - 카카오/네이버는 콜백 모양이 달라서 별도 컨트롤러로 뺄 예정(이슈 3).
 */
@RestController
@RequestMapping("/api/payment/billing-keys/toss")
@RequiredArgsConstructor
@Slf4j
public class TossBillingKeyRegistrationController {

    @Value("${payment.callback}")
    private String callbackUrl;

    private final RecurringPaymentGatewayRegistry gatewayRegistry;
    private final TossPendingRegistrationCache pendingRegistrationCache;
    private final BillingKeyRegistrationService billingKeyRegistrationService;
    private final PlanPricesService planPricesService;
    private final BillingKeysService billingKeysService;

    @PostMapping("/registrations")
    public StartRegistrationResponse startRegistration(@Valid @RequestBody StartRegistrationRequest request,
                                                         @RequestHeader("X-USER-ID") Long userId,
                                                         @RequestHeader("X-TOKEN-ID") String tokenId) {
        // userId도 클라이언트가 보낸 값을 안 믿는다 ,게이트웨이가 인증 후 실어주는 X-USER-ID 헤더에서 읽는다.
        // 첫결제인데 이미 팀에 속해있으면 여기서 예외로 막힘(재결제는 내부적으로 스킵됨)
        billingKeyRegistrationService.verifyEligibleForRegistration(userId);

        // 가격도 마찬가지로 클라이언트가 보낸 값을 안 믿고 서버가 직접 정한다 - 등록 시작 시점 가격으로 확정해서
        // 콜백 도착 전에 가격이 바뀌어도 처음 안내한 가격 그대로 유지된다(가격 고정/grandfathering).
        PlanPrices currentPrice = planPricesService.getCurrentPlanPrice(request.plan());

        RegistrationStart start = gatewayRegistry.getGateway(Provider.TOSS_PAY)
                .startRegistration(userId, callbackUrl);

        // tokenId도 같이 저장 - 콜백(토스 서버가 직접 POST) 시점엔 X-TOKEN-ID 헤더가 안 와서 지금 값을 미리 실어둬야 함.
        pendingRegistrationCache.save(start.correlationKey(), new PendingRegistration(
                userId, request.plan(), currentPrice.getAmount(), currentPrice.getId(), tokenId, RegistrationMode.NEW));

        return new StartRegistrationResponse(start.redirectUrl());
    }

    /**
     * 결제수단 변경 시작 - plan 정보 없이 PG 등록 플로우만 다시 태운다.
     * 콜백에서 mode=CHANGE로 구분해서 새 빌링키를 PENDING으로만 저장한다(즉시 교체 아님, 다음 청구 시점에 교체).
     */
    @PostMapping("/registrations/change")
    public StartRegistrationResponse startChangeBillingKey(@RequestHeader("X-USER-ID") Long userId,
                                                             @RequestHeader("X-TOKEN-ID") String tokenId) {
        // 활성 빌링키가 없거나, 이미 Toss가 활성이면(같은 PG로는 PG 정책상 재등록 불가) 여기서 막힘
        billingKeyRegistrationService.verifyEligibleForBillingKeyChange(userId, Provider.TOSS_PAY);

        RegistrationStart start = gatewayRegistry.getGateway(Provider.TOSS_PAY)
                .startRegistration(userId, callbackUrl);

        pendingRegistrationCache.save(start.correlationKey(),
                new PendingRegistration(userId, null, null, null, tokenId, RegistrationMode.CHANGE));

        return new StartRegistrationResponse(start.redirectUrl());
    }

    @PostMapping("/callback")
    public ResponseEntity<Void> handleCallback(@RequestBody Map<String, String> callbackParams) {
        String action = callbackParams.get("action");

        if ("REMOVED".equals(action)) {
            handleRemoved(callbackParams);
            return ResponseEntity.ok().build();
        }

        ConfirmedBillingKey confirmed = gatewayRegistry.getGateway(Provider.TOSS_PAY)
                .confirmRegistration(callbackParams); // 빌링키 확정

        String billingKey = callbackParams.get("billingKey");
        Optional<PendingRegistration> pending = pendingRegistrationCache.consume(billingKey);

        if (pending.isEmpty()) {
            return handleMissingPending(callbackParams, billingKey);
        }

        if (pending.get().mode() == RegistrationMode.CHANGE) {
            billingKeysService.registerPendingBillingKey(
                    pending.get().userId(), Provider.TOSS_PAY, confirmed.providerCredential(), confirmed.maskedInfo());
            return ResponseEntity.ok().build();
        }

        billingKeyRegistrationService.confirmRegistrationAndCharge(new ConfirmRegistrationCommand(
                pending.get().userId(), Provider.TOSS_PAY, confirmed.providerCredential(), confirmed.maskedInfo(),
                pending.get().plan(), pending.get().amount(), pending.get().planPriceId(), pending.get().tokenId()));

        return ResponseEntity.ok().build();
    }

    // 중복 전달(이미 활성 빌링키 존재)과 진짜 유실을 구분
    private ResponseEntity<Void> handleMissingPending(Map<String, String> callbackParams, String billingKey) {
        String userIdRaw = callbackParams.get("userId");
        boolean alreadyActive = userIdRaw != null
                && billingKeysService.findActiveByUserId(Long.valueOf(userIdRaw)).isPresent();

        if (alreadyActive) {
            log.info("토스 등록 콜백 중복 전달로 판단(이미 활성 빌링키 존재) - billingKey={}, userId={}", billingKey, userIdRaw);
            return ResponseEntity.ok().build();
        }

        log.error("[TOSS_CALLBACK_PENDING_LOST] 토스 등록 콜백에 대응하는 Redis 정보가 없고 활성 빌링키도 없음(진짜 상관관계 유실 가능성) "
                + "- billingKey={}, userId={}", billingKey, userIdRaw);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    private void handleRemoved(Map<String, String> callbackParams) {
        String userIdRaw = callbackParams.get("userId");
        if (userIdRaw == null) {
            log.warn("토스 REMOVED 콜백에 userId가 없음 - callbackParams={}", callbackParams);
            return;
        }
        billingKeyRegistrationService.revokeByProviderNotice(Long.valueOf(userIdRaw));
    }
}
