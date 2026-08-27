package com.siren.sirenpaymentapi.controller;

import com.siren.sirenpaymentapi.dto.plan_prices.PlanPriceResponse;
import com.siren.sirenpaymentapi.service.basic_service.PlanPricesService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 프론트가 가격을 표시할 때 조회하는 공개 엔드포인트
 * PlanPricesService.getCurrentPlanPrice(단일 plan)와 달리 전체 목록을 리턴한다.
 */
@RestController
@RequestMapping("/api/payment/plans")
@RequiredArgsConstructor
public class PlanPriceController {
    private final PlanPricesService planPricesService;

    @GetMapping
    public List<PlanPriceResponse> getCurrentPlanPrices() {
        return planPricesService.getAllCurrentPrices().stream()
                .map(planPrice -> new PlanPriceResponse(planPrice.getPlan(), planPrice.getAmount()))
                .toList();
    }
}
