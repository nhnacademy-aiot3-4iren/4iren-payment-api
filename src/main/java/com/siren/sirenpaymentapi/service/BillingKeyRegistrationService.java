package com.siren.sirenpaymentapi.service;

import com.siren.sirenpaymentapi.client.CoreApiClient;
import com.siren.sirenpaymentapi.domain.Plan;
import com.siren.sirenpaymentapi.domain.Provider;
import com.siren.sirenpaymentapi.domain.entity.BillingKeys;
import com.siren.sirenpaymentapi.domain.entity.PlanPrices;
import com.siren.sirenpaymentapi.domain.entity.Subscriptions;
import com.siren.sirenpaymentapi.dto.core.TeamCheckRequest;
import com.siren.sirenpaymentapi.dto.core.TeamCheckResponse;
import com.siren.sirenpaymentapi.event.RoleChangeRequested;
import com.siren.sirenpaymentapi.exception.AlreadyBelongsToTeamException;
import com.siren.sirenpaymentapi.exception.NotFoundBillingKeysException;
import com.siren.sirenpaymentapi.exception.NotFoundSubscriptionException;
import com.siren.sirenpaymentapi.gateway.RecurringPaymentGatewayRegistry;
import com.siren.sirenpaymentapi.service.basic_service.BillingKeysService;
import com.siren.sirenpaymentapi.service.basic_service.PlanPricesService;
import com.siren.sirenpaymentapi.service.basic_service.SubscriptionsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BillingKeyRegistrationService {
    private final BillingKeysService billingKeysService;
    private final SubscriptionsService subscriptionsService;
    private final PlanPricesService planPricesService;
    private final RecurringPaymentGatewayRegistry gatewayRegistry;
    private final RoleChangeEventPublisher roleChangeEventPublisher;
    private final CoreApiClient coreApiClient;

    /**
     * 등록 시작 컨트롤러(Toss/Kakao 둘 다)가 PG 호출 전에 먼저 확인.
     * 이 userId로 시도한 구독 이력이 있으면(재결제) 통과 - 이미 본인 팀 재활성화가 목적이라 체크 불필요.
     * 이력이 없으면(첫결제) Core에 팀 소속 여부를 확인해서 이미 어떤 팀에든 속해있으면 등록 자체를 거부한다.
     * Core를 못 부르면 확인 불가 상태로 등록을 허용하면 안 되므로 예외가 그대로 전파된다.
     */
    public void verifyEligibleForRegistration(Long userId) {
        if (subscriptionsService.hasSubscriptionHistory(userId)) {
            return;
        }
        TeamCheckResponse response = coreApiClient.checkTeams(new TeamCheckRequest(userId));
        if (!response.teams().isEmpty()) {
            throw new AlreadyBelongsToTeamException(userId);
        }
    }

    /**
     * PG 등록(인증) 절차가 성공으로 확정된 시점에 호출
     * 빌링키 생성 + 구독 생성을 한 트랜잭션으로 묶는다 - 최초 가입 흐름의 진입점.
     * planPriceId는 등록 시작 시점에 확정된 가격 row 참조(가격 고정/grandfathering, PendingRegistration에서 옴).
     */
    @Transactional
    public void confirmRegistration(Long userId, Provider provider, String credential, String maskedInfo,
                                    Plan plan, Long amount, Long planPriceId, String tokenId) {
        BillingKeys billingKeys = billingKeysService.registerBillingKeys(userId, provider, credential, maskedInfo);
        PlanPrices planPrice = planPricesService.getReference(planPriceId);
        subscriptionsService.registerSubscription(userId, billingKeys, planPrice, plan, amount);
        roleChangeEventPublisher.requestRoleChange(userId, RoleChangeRequested.OWNER, tokenId);
    }

    /**
     * 사용자가 결제수단을 변경할 때 호출
     * 새 빌링키 등록 + 기존 빌링키 해지 + 구독의 빌링키 재연결을 한 트랜잭션으로 묶는다.
     */
    @Transactional
    public void changeBillingKey(Long subscriptionId, Long oldBillingKeyId,
                                 Long userId, Provider provider, String newCredential, String maskedInfo) {
        BillingKeys billingKeys= billingKeysService.registerBillingKeys(userId, provider,newCredential,maskedInfo);
        billingKeysService.deleteBillingKeys(oldBillingKeyId);
        subscriptionsService.replaceBillingKey(subscriptionId, billingKeys);
    }

    /**
     * PG 쪽에서 빌링키를 강제로 삭제했을 때 호출
     * 이미 활성화된 구독이 있었던 경우에만 의미가 있다
     * markCanceled를 쓰는 이유: 이미 결제/계약된 기간이 있으므로 currentPeriodEnd까지는 계속 이용 가능해야
     * 한다
     */
    @Transactional
    public void revokeByProviderNotice(Long userId) {
        billingKeysService.findActiveByUserId(userId)
                .ifPresent(billingKeys -> billingKeysService.deleteBillingKeys(billingKeys.getId()));
        subscriptionsService.findActiveByUserId(userId)
                .ifPresent(subscriptions -> subscriptionsService.markCanceled(subscriptions.getId()));
    }

    /**
     * 사용자가 직접 구독을 해지할 때 호출
     */
    @Transactional
    public void cancelSubscription(Long userId) {
        BillingKeys billingKeys = billingKeysService.findActiveByUserId(userId)
                .orElseThrow(() -> new NotFoundBillingKeysException("user=" + userId + "의 활성 빌링키를 찾을 수 없습니다."));
        Subscriptions subscription = subscriptionsService.findActiveByUserId(userId)
                .orElseThrow(() -> new NotFoundSubscriptionException("user=" + userId + "의 활성 구독을 찾을 수 없습니다."));

        gatewayRegistry.getGateway(billingKeys.getProvider()).revoke(billingKeys.getProviderCredential());

        billingKeysService.deleteBillingKeys(billingKeys.getId());
        subscriptionsService.markCanceled(subscription.getId());
    }
}
