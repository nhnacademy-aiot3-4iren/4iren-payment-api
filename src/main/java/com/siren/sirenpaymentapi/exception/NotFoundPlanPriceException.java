package com.siren.sirenpaymentapi.exception;

import com.siren.sirenpaymentapi.domain.Plan;

//404
public class NotFoundPlanPriceException extends RuntimeException {
    public NotFoundPlanPriceException(Plan plan) {
        super("Plan=" + plan + "의 활성 가격을 찾을 수 없습니다.");
    }
}
