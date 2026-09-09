package com.siren.sirenpaymentapi.controller.toss.doc;

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

import java.util.Map;

@Tag(name = "토스페이 빌링키 등록 API", description = "토스페이 정기결제 빌링키 등록/변경/PG 콜백")
public interface TossBillingKeyRegistrationControllerDoc {

    @Operation(
            summary = "토스페이 빌링키 신규 등록 시작",
            description = "요청 시점의 요금제 가격으로 고정(grandfathering)하고, 토스페이 등록 리다이렉트 URL을 발급한다. "
                    + "이미 팀에 소속된 상태로 첫결제를 시도하면 실패한다."
    )
    @ApiResponse(responseCode = "200", description = "리다이렉트 URL 발급 성공")
    StartRegistrationResponse startRegistration(
            @Valid @RequestBody StartRegistrationRequest request,
            @Parameter(hidden = true) @RequestHeader("X-USER-ID") Long userId,
            @Parameter(hidden = true) @RequestHeader("X-TOKEN-ID") String tokenId
    );

    @Operation(
            summary = "결제수단(토스페이) 변경 시작",
            description = "활성 구독이 있는 유저가 결제수단을 토스페이로 변경할 때 등록 플로우를 다시 태운다. "
                    + "콜백에서 확정되면 즉시 교체가 아니라 다음 청구 시점에 새 빌링키로 교체된다."
    )
    @ApiResponse(responseCode = "200", description = "리다이렉트 URL 발급 성공")
    StartRegistrationResponse startChangeBillingKey(
            @Parameter(hidden = true) @RequestHeader("X-USER-ID") Long userId,
            @Parameter(hidden = true) @RequestHeader("X-TOKEN-ID") String tokenId
    );

    @Operation(
            summary = "토스페이 빌링키 등록 콜백",
            description = "PG 프론트엔드가 아니라 토스페이 서버가 등록 결과를 알리기 위해 직접 호출하는 웹훅. "
                    + "프론트/클라이언트가 호출할 API가 아니며, action=REMOVED면 빌링키 해지 통보로 처리한다."
    )
    @ApiResponse(responseCode = "200", description = "처리 완료")
    @ApiResponse(responseCode = "404", description = "상관관계 정보 유실(재현 시 원인 파악 필요)")
    ResponseEntity<Void> handleCallback(@RequestBody Map<String, String> callbackParams);
}
