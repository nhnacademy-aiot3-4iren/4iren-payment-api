package com.siren.sirenpaymentapi.dto.plan_prices;

import com.siren.sirenpaymentapi.domain.Plan;

public record PlanPriceResponse(Plan plan, Long amount) {
}
