package com.siren.sirenpaymentapi.service.basic_service;

import com.siren.sirenpaymentapi.domain.Plan;
import com.siren.sirenpaymentapi.domain.PlanPriceStatus;
import com.siren.sirenpaymentapi.domain.entity.PlanPrices;
import com.siren.sirenpaymentapi.exception.NotFoundPlanPriceException;
import com.siren.sirenpaymentapi.repository.PlanPricesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlanPricesService {
    private final PlanPricesRepository planPricesRepository;

    // 프론트에 요금제 목록을 보여줄 때 호출 (PlanPriceController)
    @Transactional(readOnly = true)
    public List<PlanPrices> getAllCurrentPrices() {
        return planPricesRepository.findByStatus(PlanPriceStatus.ACTIVE);
    }

    /**
     * 등록 시작 시 서버가 직접 가격을 정할 때 호출
     */
    @Transactional(readOnly = true)
    public PlanPrices getCurrentPlanPrice(Plan plan) {
        return planPricesRepository.findByPlanAndStatus(plan, PlanPriceStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundPlanPriceException(plan));
    }
    public PlanPrices getReference(Long planPriceId) {
        return planPricesRepository.getReferenceById(planPriceId);
    }

    /** 기존 ACTIVE row를 INACTIVE로 전환 + 새 row를 ACTIVE로 추가,
     * 한 트랜잭션으로 묶어서 plan당 ACTIVE row 하나 불변식이 깨지지 않게 한다.
     */
    @Transactional
    public void changePrice(Plan plan, Long newAmount) {
        planPricesRepository.findByPlanAndStatus(plan, PlanPriceStatus.ACTIVE)
                .ifPresent(PlanPrices::markInactive);

        planPricesRepository.save(PlanPrices.builder()
                .plan(plan)
                .amount(newAmount)
                .status(PlanPriceStatus.ACTIVE)
                .build());
    }
}
