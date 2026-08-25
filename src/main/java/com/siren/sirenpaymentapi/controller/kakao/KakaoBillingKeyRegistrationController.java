package com.siren.sirenpaymentapi.controller.kakao;

import com.siren.sirenpaymentapi.domain.Provider;
import com.siren.sirenpaymentapi.domain.entity.PlanPrices;
import com.siren.sirenpaymentapi.dto.billing_keys.ConfirmRegistrationCommand;
import com.siren.sirenpaymentapi.dto.billing_keys.StartRegistrationRequest;
import com.siren.sirenpaymentapi.dto.billing_keys.StartRegistrationResponse;
import com.siren.sirenpaymentapi.dto.gateway.ConfirmedBillingKey;
import com.siren.sirenpaymentapi.dto.gateway.RegistrationStart;
import com.siren.sirenpaymentapi.dto.kakao.PendingRegistration;
import com.siren.sirenpaymentapi.gateway.RecurringPaymentGatewayRegistry;
import com.siren.sirenpaymentapi.service.BillingKeyRegistrationService;
import com.siren.sirenpaymentapi.service.basic_service.PlanPricesService;
import com.siren.sirenpaymentapi.service.cache.KakaoPendingRegistrationCache;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

/**
 * 카카오페이 빌링키 등록 흐름 전용 컨트롤러.
 */
@RestController
@RequestMapping("/api/payments/billing-keys/kakao")
@RequiredArgsConstructor
@Slf4j
public class KakaoBillingKeyRegistrationController {

    @Value("${payment.kakao-callback}")
    private String callbackBaseUrl;

    @Value("${payment.successUrl}")
    private String successUrl;

    @Value("${payment.failureUrl}")
    private String failureUrl;

    private final RecurringPaymentGatewayRegistry gatewayRegistry;
    private final KakaoPendingRegistrationCache pendingRegistrationCache;
    private final BillingKeyRegistrationService billingKeyRegistrationService;
    private final PlanPricesService planPricesService;

    @PostMapping("/registrations")
    public StartRegistrationResponse startRegistration(@Valid @RequestBody StartRegistrationRequest request,
                                                         @RequestHeader("X-USER-ID") Long userId,
                                                         @RequestHeader("X-TOKEN-ID") String tokenId) {
        // userId도 클라이언트가 보낸 값을 안 믿는다 - 게이트웨이가 인증 후 실어주는 X-USER-ID 헤더에서 읽는다.
        // 첫결제인데 이미 팀에 속해있으면 여기서 예외로 막힘(재결제는 내부적으로 스킵됨)
        billingKeyRegistrationService.verifyEligibleForRegistration(userId);

        // 가격도 마찬가지로 클라이언트가 보낸 값을 안 믿고 서버가 직접 정한다 - 등록 시작 시점 가격으로 확정(가격 고정/grandfathering).
        PlanPrices currentPrice = planPricesService.getCurrentPlanPrice(request.plan());

        RegistrationStart start = gatewayRegistry.getGateway(Provider.KAKAO_PAY)
                .startRegistration(userId, callbackBaseUrl);

        // correlationKey=orderId, providerReference=tid - 콜백 때 approve 호출에 tid가 필수라 같이 저장.
        // tokenId도 같이 저장 - 콜백(PG 리다이렉트) 시점엔 X-TOKEN-ID 헤더가 안 와서 지금 값을 미리 실어둬야 함.
        pendingRegistrationCache.save(start.correlationKey(), new PendingRegistration(
                userId, request.plan(), currentPrice.getAmount(), currentPrice.getId(),
                start.providerReference(), tokenId));

        return new StartRegistrationResponse(start.redirectUrl());
    }

    /**
     * approval_url 자체 - 카카오가 사용자 브라우저를 pg_token과 함께 여기로 리다이렉트한다.
     * 처리 후 사람이 보는 성공/실패 페이지로 다시 리다이렉트한다(Toss와 달리 이 엔드포인트 자체가 브라우저 화면).
     */
    @GetMapping("/callback")
    public ResponseEntity<Void> handleCallback(@RequestParam("pg_token") String pgToken,
                                                @RequestParam("orderId") String orderId) {
        Optional<PendingRegistration> pending = pendingRegistrationCache.consume(orderId);

        if (pending.isEmpty()) {
            log.warn("카카오 등록 콜백을 받았는데 대응하는 Redis 정보가 없음(TTL 만료 또는 중복 처리) - orderId={}", orderId);
            return redirectTo(failureUrl);
        }

        try {
            Map<String, String> callbackParams = Map.of(
                    "tid", pending.get().tid(),
                    "orderId", orderId,
                    "userId", String.valueOf(pending.get().userId()),
                    "pg_token", pgToken
            );

            ConfirmedBillingKey confirmed = gatewayRegistry.getGateway(Provider.KAKAO_PAY)
                    .confirmRegistration(callbackParams);

            billingKeyRegistrationService.confirmRegistration(new ConfirmRegistrationCommand(
                    pending.get().userId(), Provider.KAKAO_PAY, confirmed.providerCredential(), confirmed.maskedInfo(),
                    pending.get().plan(), pending.get().amount(), pending.get().planPriceId(), pending.get().tokenId()));

            return redirectTo(successUrl);
        } catch (Exception e) {
            log.error("카카오 등록 확정 처리 중 예외 발생 - orderId={}", orderId, e);
            return redirectTo(failureUrl);
        }
    }

    private ResponseEntity<Void> redirectTo(String url) {
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(url)).build();
    }
}
