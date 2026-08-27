package com.siren.sirenpaymentapi.dto.billing_keys;

import com.siren.sirenpaymentapi.domain.Plan;
import jakarta.validation.constraints.NotNull;

// amount는 안 받는다 - 클라이언트가 보낸 금액을 그대로 신뢰하면 가격 조작 위험이 있어서
// 서버가 PlanPricesService로 직접 정한다(BillingKeyRegistrationController.startRegistration).
// userId도 안 받는다 - 같은 이유(남의 userId로 등록을 시작할 수 있는 위험). 게이트웨이가 인증 후 실어주는
// X-USER-ID 헤더에서 컨트롤러가 직접 읽는다.
public record StartRegistrationRequest(
        @NotNull Plan plan
) {
}
