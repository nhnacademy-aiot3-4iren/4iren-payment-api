package com.siren.sirenpaymentapi.controller.kakao.doc;

import com.siren.sirenpaymentapi.dto.billing_keys.StartRegistrationRequest;
import com.siren.sirenpaymentapi.dto.billing_keys.StartRegistrationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "카카오페이 빌링키 등록 API", description = "카카오페이 정기결제 빌링키 등록/변경/PG 콜백")
public interface KakaoBillingKeyRegistrationControllerDoc {

    @Operation(
            summary = "카카오페이 빌링키 신규 등록 시작",
            description = "요청 시점의 요금제 가격으로 고정(grandfathering)하고, 카카오페이 등록 리다이렉트 URL을 발급한다. "
                    + "이미 팀에 소속된 상태로 첫결제를 시도하면 실패한다."
    )
    @ApiResponse(responseCode = "200", description = "리다이렉트 URL 발급 성공")
    StartRegistrationResponse startRegistration(
            @Valid @RequestBody StartRegistrationRequest request,
            @Parameter(hidden = true) @RequestHeader("X-USER-ID") Long userId,
            @Parameter(hidden = true) @RequestHeader("X-TOKEN-ID") String tokenId
    );

    @Operation(
            summary = "결제수단(카카오페이) 변경 시작",
            description = "활성 구독이 있는 유저가 결제수단을 카카오페이로 변경할 때 등록 플로우를 다시 태운다. "
                    + "콜백에서 확정되면 즉시 교체가 아니라 다음 청구 시점에 새 빌링키로 교체된다."
    )
    @ApiResponse(responseCode = "200", description = "리다이렉트 URL 발급 성공")
    StartRegistrationResponse startChangeBillingKey(
            @Parameter(hidden = true) @RequestHeader("X-USER-ID") Long userId,
            @Parameter(hidden = true) @RequestHeader("X-TOKEN-ID") String tokenId
    );

    @Operation(
            summary = "카카오페이 빌링키 등록 콜백",
            description = "카카오페이가 pg_token과 함께 사용자 브라우저를 리다이렉트시키는 approval_url. "
                    + "프론트/클라이언트가 직접 호출할 API가 아니며, 성공/실패 페이지 분기는 이 엔드포인트를 실제로 받는 front-server가 결정한다."
    )
    @ApiResponse(responseCode = "200", description = "처리 완료")
    @ApiResponse(responseCode = "404", description = "상관관계 정보 유실(TTL 만료 또는 중복 처리)")
    ResponseEntity<Void> handleCallback(
            @Parameter(description = "카카오페이 결제 승인 토큰", required = true) @RequestParam("pg_token") String pgToken,
            @Parameter(description = "등록 시작 시 발급된 상관관계 키", required = true) @RequestParam("orderId") String orderId
    );
}
