package com.siren.sirenpaymentapi.controller.doc;

import com.siren.sirenpaymentapi.dto.subscriptions.SubscriptionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;

@Tag(name = "구독 API", description = "PG 무관 구독 조회/해지 API")
public interface SubscriptionControllerDoc {

    @Operation(
            summary = "내 구독 조회",
            description = "로그인한 유저(X-USER-ID)의 최신 구독 정보를 조회한다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    ResponseEntity<SubscriptionResponse> getCurrentSubscription(
            @Parameter(hidden = true) @RequestHeader("X-USER-ID") Long userId
    );

    @Operation(
            summary = "구독 해지",
            description = "로그인한 유저(X-USER-ID)의 구독을 해지한다. 즉시 하드 삭제가 아니라 상태 전이(CANCELED 등)로만 처리된다."
    )
    @ApiResponse(responseCode = "204", description = "해지 성공")
    ResponseEntity<Void> cancelSubscription(
            @Parameter(hidden = true) @RequestHeader("X-USER-ID") Long userId
    );
}
