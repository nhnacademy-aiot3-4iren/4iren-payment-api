package com.siren.sirenpaymentapi.controller.doc;

import com.siren.sirenpaymentapi.dto.plan_prices.PlanPriceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(name = "요금제 가격 API", description = "요금제별 현재 가격 조회 API (공개)")
public interface PlanPriceControllerDoc {

    @Operation(
            summary = "요금제별 현재 가격 목록 조회",
            description = "인증 없이 호출 가능한 공개 엔드포인트. 프론트가 가격 표시에 사용하는 전체 요금제 가격 목록을 조회한다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    List<PlanPriceResponse> getCurrentPlanPrices();
}
