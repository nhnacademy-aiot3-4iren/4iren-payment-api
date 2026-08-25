package com.siren.sirenpaymentapi.repository;

import com.siren.sirenpaymentapi.domain.Plan;
import com.siren.sirenpaymentapi.domain.PlanPriceStatus;
import com.siren.sirenpaymentapi.domain.entity.PlanPrices;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlanPricesRepository extends JpaRepository<PlanPrices, Long> {
    Optional<PlanPrices> findByPlanAndStatus(Plan plan, PlanPriceStatus status);

    // 프론트에 요금제 전체를 보여줄 때 사용(PlanPriceController)
    List<PlanPrices> findByStatus(PlanPriceStatus status);
}
