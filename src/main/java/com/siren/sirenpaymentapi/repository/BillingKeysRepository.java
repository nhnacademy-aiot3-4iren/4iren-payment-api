package com.siren.sirenpaymentapi.repository;

import com.siren.sirenpaymentapi.domain.BillingKeyStatus;
import com.siren.sirenpaymentapi.domain.entity.BillingKeys;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BillingKeysRepository extends JpaRepository<BillingKeys, Long> {

    // 유저당 활성 빌링키는 하나뿐이라는 전제 - 토스 REMOVED 콜백 처리 시 역조회용
    Optional<BillingKeys> findByUserIdAndStatus(Long userId, BillingKeyStatus status);
}
