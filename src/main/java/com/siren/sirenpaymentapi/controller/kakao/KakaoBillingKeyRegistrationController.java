package com.siren.sirenpaymentapi.controller.kakao;

import com.siren.sirenpaymentapi.domain.Provider;
import com.siren.sirenpaymentapi.domain.RegistrationMode;
import com.siren.sirenpaymentapi.domain.entity.PlanPrices;
import com.siren.sirenpaymentapi.dto.billing_keys.ConfirmRegistrationCommand;
import com.siren.sirenpaymentapi.dto.billing_keys.StartRegistrationRequest;
import com.siren.sirenpaymentapi.dto.billing_keys.StartRegistrationResponse;
import com.siren.sirenpaymentapi.dto.gateway.ConfirmedBillingKey;
import com.siren.sirenpaymentapi.dto.gateway.RegistrationStart;
import com.siren.sirenpaymentapi.dto.kakao.PendingRegistration;
import com.siren.sirenpaymentapi.gateway.RecurringPaymentGatewayRegistry;
import com.siren.sirenpaymentapi.security.RequireRole;
import com.siren.sirenpaymentapi.security.Role;
import com.siren.sirenpaymentapi.service.BillingKeyRegistrationService;
import com.siren.sirenpaymentapi.service.basic_service.BillingKeysService;
import com.siren.sirenpaymentapi.service.basic_service.PlanPricesService;
import com.siren.sirenpaymentapi.service.cache.KakaoPendingRegistrationCache;
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
 * 카카오페이 빌링키 등록 흐름 전용 컨트롤러.
 */
@RestController
@RequestMapping("/api/payment/billing-keys/kakao")
@RequiredArgsConstructor
@Slf4j
public class KakaoBillingKeyRegistrationController {

    @Value("${payment.kakao-callback}")
    private String callbackBaseUrl;

    private final RecurringPaymentGatewayRegistry gatewayRegistry;
    private final KakaoPendingRegistrationCache pendingRegistrationCache;
    private final BillingKeyRegistrationService billingKeyRegistrationService;
    private final PlanPricesService planPricesService;
    private final BillingKeysService billingKeysService;

    @RequireRole(value = Role.NORMAL)
    @PostMapping("/registrations")
    public StartRegistrationResponse startRegistration(@Valid @RequestBody StartRegistrationRequest request,
                                                         @RequestHeader("X-USER-ID") Long userId,
                                                         @RequestHeader("X-TOKEN-ID") String tokenId) {
        // 첫결제인데 이미 팀에 속해있으면 여기서 예외로 막힘(재결제는 내부적으로 스킵됨)
        billingKeyRegistrationService.verifyEligibleForRegistration(userId);

        // 등록 시작 시점 가격으로 확정(가격 고정/grandfathering).
        PlanPrices currentPrice = planPricesService.getCurrentPlanPrice(request.plan());

        RegistrationStart start = gatewayRegistry.getGateway(Provider.KAKAO_PAY)
                .startRegistration(userId, callbackBaseUrl);

        // correlationKey=orderId, providerReference=tid - 콜백 때 approve 호출에 tid가 필수라 같이 저장.
        // tokenId도 같이 저장 - 콜백(PG 리다이렉트) 시점엔 X-TOKEN-ID 헤더가 안 와서 지금 값을 미리 실어둬야 함.
        pendingRegistrationCache.save(start.correlationKey(), new PendingRegistration(
                userId, request.plan(), currentPrice.getAmount(), currentPrice.getId(),
                start.providerReference(), tokenId, RegistrationMode.NEW));

        return new StartRegistrationResponse(start.redirectUrl());
    }

    /**
     * 결제수단 변경 시작 - plan 정보 없이 PG 등록 플로우만 다시 태운다.
     * 콜백에서 mode=CHANGE로 구분해서 새 빌링키를 PENDING으로만 저장한다(즉시 교체 아님, 다음 청구 시점에 교체).
     */
    @RequireRole(value = Role.OWNER)
    @PostMapping("/registrations/change")
    public StartRegistrationResponse startChangeBillingKey(@RequestHeader("X-USER-ID") Long userId,
                                                             @RequestHeader("X-TOKEN-ID") String tokenId) {
        // 활성 빌링키가 없거나, 이미 Kakao가 활성이면(같은 PG로는 PG 정책상 재등록 불가) 여기서 막힘
        billingKeyRegistrationService.verifyEligibleForBillingKeyChange(userId, Provider.KAKAO_PAY);

        RegistrationStart start = gatewayRegistry.getGateway(Provider.KAKAO_PAY)
                .startRegistration(userId, callbackBaseUrl);

        pendingRegistrationCache.save(start.correlationKey(), new PendingRegistration(
                userId, null, null, null, start.providerReference(), tokenId, RegistrationMode.CHANGE));

        return new StartRegistrationResponse(start.redirectUrl());
    }

    /**
     * approval_url 자체 - 카카오가 사용자 브라우저를 pg_token과 함께 여기로 리다이렉트한다.
     * 성공/실패 페이지로의 리다이렉트는 이 엔드포인트를 실제로 받는 front-server가 결정 - 여긴 상태코드만 응답한다.
     */
    @GetMapping("/callback")
    public ResponseEntity<Void> handleCallback(@RequestParam("pg_token") String pgToken,
                                                @RequestParam("orderId") String orderId) {
        Optional<PendingRegistration> pending = pendingRegistrationCache.consume(orderId);

        if (pending.isEmpty()) {
            log.warn("카카오 등록 콜백을 받았는데 대응하는 Redis 정보가 없음(TTL 만료 또는 중복 처리) - orderId={}", orderId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Map<String, String> callbackParams = Map.of(
                "tid", pending.get().tid(),
                "orderId", orderId,
                "userId", String.valueOf(pending.get().userId()),
                "pg_token", pgToken
        );

        ConfirmedBillingKey confirmed = gatewayRegistry.getGateway(Provider.KAKAO_PAY)
                .confirmRegistration(callbackParams);

        if (pending.get().mode() == RegistrationMode.CHANGE) {
            billingKeysService.registerPendingBillingKey(
                    pending.get().userId(), Provider.KAKAO_PAY, confirmed.providerCredential(), confirmed.maskedInfo());
            return ResponseEntity.ok().build();
        }

        billingKeyRegistrationService.confirmRegistrationAndCharge(new ConfirmRegistrationCommand(
                pending.get().userId(), Provider.KAKAO_PAY, confirmed.providerCredential(), confirmed.maskedInfo(),
                pending.get().plan(), pending.get().amount(), pending.get().planPriceId(), pending.get().tokenId()));

        return ResponseEntity.ok().build();
    }
}
