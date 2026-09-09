package com.siren.sirenpaymentapi.controller.doc;

import com.siren.sirenpaymentapi.dto.payments.PaymentHistoryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@Tag(name = "결제 이력 API", description = "유저의 결제 이력 조회 API")
public interface PaymentHistoryControllerDoc {

    @Operation(
            summary = "내 결제 이력 목록 조회",
            description = "로그인한 유저(X-USER-ID)의 결제 이력을 전체 조회한다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    List<PaymentHistoryResponse> getMyPayments(
            @Parameter(hidden = true) @RequestHeader("X-USER-ID") Long userId
    );
}
