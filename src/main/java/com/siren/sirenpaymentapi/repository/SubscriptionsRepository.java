package com.siren.sirenpaymentapi.repository;

import com.siren.sirenpaymentapi.domain.SubscriptionStatus;
import com.siren.sirenpaymentapi.domain.entity.Subscriptions;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface SubscriptionsRepository extends JpaRepository<Subscriptions, Long>, SubscriptionsRepositoryCustom {

    // 정기청구 처리 중 같은 구독이 동시에 처리되지 않도록 비관적 쓰기 잠금을 적용한 조회
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Subscriptions> findLockedById(Long id);

    // 유저당 활성 구독은 하나뿐이라는 전제 - 토스 REMOVED 콜백 처리 시 역조회용
    Optional<Subscriptions> findByUserIdAndStatus(Long userId, SubscriptionStatus status);

    // 첫결제/재결제 구분용 - 이 userId로 시도한 구독 이력이 하나라도 있는지(상태 무관)
    boolean existsByUserId(Long userId);

    // "현재 이용 중인 요금제" 조회용 - ACTIVE/PAST_DUE/CANCELED(해지 접수했지만 기간 안 끝남) 상태 무관하게
    // 최신 구독 1건. OWNER만 호출 가능한 엔드포인트라 EXPIRED로 이미 강등된 유저는 애초에 못 들어옴.
    Optional<Subscriptions> findFirstByUserIdOrderByCreatedAtDesc(Long userId);
}
