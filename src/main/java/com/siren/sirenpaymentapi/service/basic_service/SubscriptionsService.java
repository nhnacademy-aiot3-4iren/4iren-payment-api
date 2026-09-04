package com.siren.sirenpaymentapi.service.basic_service;

import com.siren.sirenpaymentapi.domain.Plan;
import com.siren.sirenpaymentapi.domain.SubscriptionStatus;
import com.siren.sirenpaymentapi.domain.entity.BillingKeys;
import com.siren.sirenpaymentapi.domain.entity.PlanPrices;
import com.siren.sirenpaymentapi.domain.entity.Subscriptions;
import com.siren.sirenpaymentapi.dto.mail.CanceledMailContext;
import com.siren.sirenpaymentapi.dto.mail.ExpiredMailContext;
import com.siren.sirenpaymentapi.dto.mail.PastDueMailContext;
import com.siren.sirenpaymentapi.dto.mail.SubEndedMailContext;
import com.siren.sirenpaymentapi.dto.subscriptions.BillingTarget;
import com.siren.sirenpaymentapi.dto.subscriptions.SubscriptionResponse;
import com.siren.sirenpaymentapi.event.RoleChangeRequested;
import com.siren.sirenpaymentapi.exception.NotFoundSubscriptionException;
import com.siren.sirenpaymentapi.mail.MailEventPublisher;
import com.siren.sirenpaymentapi.repository.SubscriptionsRepository;
import com.siren.sirenpaymentapi.service.RoleChangeEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class SubscriptionsService {
    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");
    private final SubscriptionsRepository subscriptionsRepository;
    private final RoleChangeEventPublisher roleChangeEventPublisher;
    private final MailEventPublisher mailEventPublisher;

    // 재시도 3번까지만(매일 시도) config로 외부화하되 지금은 기본값만 씀(config-repo 반영 안 함)
    @Value("${payment.dunning.max-retry-count:3}")
    private int maxRetryCount;

    // 토스 REMOVED 콜백 처리 시 userId로 지금 활성화된 구독을 찾기 위해 사용
    public Optional<Subscriptions> findActiveByUserId(Long userId) {
        return subscriptionsRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE);
    }

    // "현재 이용 중인 요금제" 조회용 - findActiveByUserId와 달리 ACTIVE로 안 좁힘(PAST_DUE/CANCELED도 조회돼야 함)
    public SubscriptionResponse findLatestByUserId(Long userId) {
        Subscriptions subscription = subscriptionsRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> new NotFoundSubscriptionException("user: " + userId + "의 구독을 찾을 수 없습니다."));

        return new SubscriptionResponse(
                subscription.getPlan(), subscription.getAmount(), subscription.getStatus(),
                subscription.getCurrentPeriodEnd(), subscription.getNextBillingDate());
    }

    // 가입 직후 첫 청구 성공 메일에 nextBillingDate를 실어보내려는 단순 조회용(registerSubscription이 계산한 값 재사용)
    @Transactional(readOnly = true)
    public Subscriptions getById(Long subscriptionId) {
        return subscriptionsRepository.findById(subscriptionId)
                .orElseThrow(() -> new NotFoundSubscriptionException(subscriptionId));
    }

    // 등록 시작 컨트롤러가 호출 - 첫결제(팀 소속 여부를 Core에 확인해야 함)인지 재결제(스킵)인지 구분
    @Transactional(readOnly = true)
    public boolean hasSubscriptionHistory(Long userId) {
        return subscriptionsRepository.existsByUserId(userId);
    }

    /**
     * 최초 구독 생성. BillingKeyRegistrationService.confirmRegistration에서
     * 빌링키 등록 직후 같은 트랜잭션으로 호출됨. plan에 따라 첫 결제 주기(currentPeriodEnd/nextBillingDate)를 계산해 ACTIVE 상태로 저장한다.
     * planPrice는 가입 시점 가격 고정(grandfathering)용 참조 - 이후 가격이 바뀌어도 이 구독엔 영향 없음.
     */
    public Subscriptions registerSubscription(Long userId, BillingKeys billingKeys, PlanPrices planPrice,
                                               Plan plan, Long amount) {
        LocalDateTime periodEnd = plan == Plan.MONTHLY
                ? LocalDateTime.now(ZONE_ID).plusMonths(1)
                : LocalDateTime.now(ZONE_ID).plusYears(1);

        Subscriptions subscriptions = Subscriptions.builder()
                .userId(userId)
                .billingKey(billingKeys)
                .planPrice(planPrice)
                .plan(plan)
                .amount(amount)
                .status(SubscriptionStatus.ACTIVE)
                .currentPeriodEnd(periodEnd)
                .nextBillingDate(periodEnd.toLocalDate())
                .build();

        return subscriptionsRepository.save(subscriptions);
    }

    /**
     * 결제수단 변경 시 호출. BillingKeyRegistrationService.changeBillingKey에서
     * 새 빌링키 등록 + 기존 빌링키 해지와 같은 트랜잭션으로 묶여서 실행됨. 구독이 가리키는 빌링키만 교체(주기/금액은 안 건드림).
     */
    @Transactional
    public void replaceBillingKey(Long subscriptionId, BillingKeys billingKeys) {
        findLocked(subscriptionId).replaceBillingKey(billingKeys);
    }

    /**
     * 정기 청구가 정상(연체 아니었음) 성공했을 때 호출.
     * currentPeriodEnd/nextBillingDate를 다음 주기로 진행시킨다.
     */
    @Transactional
    public Subscriptions advanceBillingCycle(Long subscriptionId){
        Subscriptions subscriptions = findLocked(subscriptionId);
        subscriptions.advanceBillingCycle();
        return subscriptions;
    }

    /**
     * PAST_DUE(유예기간) 상태에서 재시도 청구가 성공했을 때 호출.
     * status를 ACTIVE로 되돌리고 retryCount를 0으로 리셋한 뒤 다음 주기를 계산한다.
     */
    @Transactional
    public Subscriptions recoverActive(Long subscriptionId){
        Subscriptions subscriptions = findLocked(subscriptionId);
        subscriptions.recoverActive();
        return subscriptions;
    }

    /**
     * 정기 청구가 실패했을 때 호출.
     * retryCount가 이미 maxRetryCount에 도달했으면(재시도를 다 쓰고도 또 실패) PAST_DUE 대신 즉시 EXPIRED로 전이.
     * 아니면 PAST_DUE로 전이시키고 retryCount를 1 증가(role/Account엔 PAST_DUE 자체는 아직 영향 없음).
     */
    @Transactional
    public boolean markPastDue(Long subscriptionId, String failureReason){
        Subscriptions subscriptions = findLocked(subscriptionId);
        if (subscriptions.getRetryCount() >= maxRetryCount) {
            expireAndPublish(subscriptions, failureReason);
            return true;
        }
        subscriptions.markPastDue();
        mailEventPublisher.notify(subscriptions.getUserId(), new PastDueMailContext(
                subscriptions.getPlan().name(), LocalDateTime.now(ZONE_ID), subscriptions.getNextBillingDate(),
                subscriptions.getRetryCount(), maxRetryCount, failureReason));
        return false;
    }

    /**
     * PAST_DUE 유예기간을 다 소진했을 때(Dunning 재시도 초과) markPastDue()가 호출 -
     * status를 EXPIRED로 전이시키고 Account로 OWNER 강등 이벤트를 같이 발행한다. 스케줄러가 혼자 도는
     * 배치라 살아있는 토큰이 없어서 tokenId는 null - Account가 tokenId 없을 땐 특정 토큰 하나가 아니라
     * 해당 유저의 전체 세션을 무효화해야 함.
     */
    @Transactional
    public void markExpired(Long subscriptionId){
        expireAndPublish(findLocked(subscriptionId), "결제 실패로 인한 만료");
    }

    private void expireAndPublish(Subscriptions subscriptions, String failureReason) {
        subscriptions.markExpired();
        roleChangeEventPublisher.requestRoleChange(subscriptions.getUserId(), RoleChangeRequested.NORMAL, null);
        mailEventPublisher.notify(subscriptions.getUserId(), new ExpiredMailContext(
                subscriptions.getPlan().name(), LocalDateTime.now(ZONE_ID), failureReason));
    }

    /**
     * 가입 직후 첫 청구가 실패했을 때 호출(BillingKeyRegistrationService.confirmRegistrationAndCharge).
     * 아직 OWNER로 승격된 적이 없으므로 강등 이벤트는 발행하지 않는다 - markExpired()만으로 종결.
     */
    @Transactional
    public void failInitialCharge(Long subscriptionId) {
        findLocked(subscriptionId).markExpired();
    }

    /**
     * 사용자가 직접 구독 해지를 요청했을 때 호출
     * 즉시 만료가 아니라 currentPeriodEnd까지는 계속 이용 가능한 채로 status만 CANCELED로 전이한다.
     */
    @Transactional
    public void markCanceled(Long subscriptionId){
        Subscriptions subscriptions = findLocked(subscriptionId);
        subscriptions.markCanceled();
        mailEventPublisher.notify(subscriptions.getUserId(), new CanceledMailContext(
                subscriptions.getPlan().name(), LocalDateTime.now(ZONE_ID), subscriptions.getCurrentPeriodEnd()));
    }

    /**
     * 해지(CANCELED)된 구독의 계약기간(currentPeriodEnd)이
     * 실제로 끝났을 때. markExpired()와 달리 status는 CANCELED 그대로 두고(결제 실패로 인한 만료와는 다른
     * 사유라서 status만으로 구분 가능해야 함) roleDowngradedAt만 기록 - 이 필드가 채워지는 게
     * findCanceledPastPeriodEnd 쿼리에서 다음날 또 조회되지 않게 막는 표시 역할도 겸함.
     * Account 강등 이벤트는 markExpired()와 동일하게 tokenId=null로 발행(스케줄러 배치라 살아있는 토큰 없음).
     */
    @Transactional
    public void downgradeAfterCancelation(Long subscriptionId){
        Subscriptions subscriptions = findLocked(subscriptionId);
        subscriptions.markRoleDowngraded();
        roleChangeEventPublisher.requestRoleChange(subscriptions.getUserId(), RoleChangeRequested.NORMAL, null);
        mailEventPublisher.notify(subscriptions.getUserId(), new SubEndedMailContext(
                subscriptions.getPlan().name(), LocalDateTime.now(ZONE_ID)));
    }

    /**
     * 자동청구 스케줄러가 호출
     */
    @Transactional(readOnly = true)
    public List<BillingTarget> findDueForBilling(LocalDate billingDate) {
        return subscriptionsRepository.findDueForBilling(billingDate).stream()
                .map(subscriptions -> new BillingTarget(
                        subscriptions.getId(),
                        subscriptions.getUserId(),
                        subscriptions.getBillingKey().getProvider(),
                        subscriptions.getBillingKey().getProviderCredential(),
                        subscriptions.getAmount(),
                        subscriptions.getStatus() == SubscriptionStatus.PAST_DUE))
                .toList();
    }

    /**
     * 해지(CANCELED)됐고 계약기간(currentPeriodEnd)이 지났는데
     * 아직 downgradeAfterCancelation()이 안 태워진(roleDowngradedAt이 null인) 구독들을 찾는다.
     */
    @Transactional(readOnly = true)
    public List<Long> findCanceledPastPeriodEnd(LocalDateTime cutoff) {
        return subscriptionsRepository.findCanceledPastPeriodEnd(cutoff);
    }

    private Subscriptions findLocked(Long subscriptionId) {
        return subscriptionsRepository.findLockedById(subscriptionId)
                .orElseThrow(()-> new NotFoundSubscriptionException(subscriptionId));
    }

}
