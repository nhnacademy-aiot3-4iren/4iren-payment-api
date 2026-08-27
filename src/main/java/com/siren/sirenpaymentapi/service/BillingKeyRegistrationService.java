package com.siren.sirenpaymentapi.service;

import com.siren.sirenpaymentapi.client.CoreApiClient;
import com.siren.sirenpaymentapi.domain.Provider;
import com.siren.sirenpaymentapi.domain.entity.BillingKeys;
import com.siren.sirenpaymentapi.domain.entity.PlanPrices;
import com.siren.sirenpaymentapi.domain.entity.Subscriptions;
import com.siren.sirenpaymentapi.dto.billing_keys.ConfirmRegistrationCommand;
import com.siren.sirenpaymentapi.dto.core.TeamCheckRequest;
import com.siren.sirenpaymentapi.dto.core.TeamCheckResponse;
import com.siren.sirenpaymentapi.dto.gateway.ActiveBillingKey;
import com.siren.sirenpaymentapi.dto.gateway.ChargeResult;
import com.siren.sirenpaymentapi.dto.payments.PreparedCharge;
import com.siren.sirenpaymentapi.dto.subscriptions.BillingTarget;
import com.siren.sirenpaymentapi.event.RoleChangeRequested;
import com.siren.sirenpaymentapi.exception.AlreadyBelongsToTeamException;
import com.siren.sirenpaymentapi.exception.InitialChargeFailedException;
import com.siren.sirenpaymentapi.exception.NotFoundBillingKeysException;
import com.siren.sirenpaymentapi.exception.NotFoundSubscriptionException;
import com.siren.sirenpaymentapi.exception.SameProviderBillingKeyChangeException;
import com.siren.sirenpaymentapi.gateway.RecurringPaymentGatewayRegistry;
import com.siren.sirenpaymentapi.service.basic_service.BillingKeysService;
import com.siren.sirenpaymentapi.service.basic_service.PaymentsService;
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
    private final PaymentsService paymentsService;
    private final SubscriptionChargeService subscriptionChargeService;
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
     * OWNER 승격은 여기서 하지 않는다 - confirmRegistrationAndCharge가 첫 청구 성공을 확인한 뒤에 한다.
     * 반환값은 방금 만든 구독을 바로 청구하는 데 필요한 스칼라만 담은 BillingTarget(트랜잭션 밖으로 detached
     * 엔티티를 넘기면 LazyInitializationException이 나므로).
     */
    @Transactional
    public BillingTarget confirmRegistration(ConfirmRegistrationCommand command) {
        BillingKeys billingKeys = billingKeysService.registerBillingKeys(
                command.userId(), command.provider(), command.providerCredential(), command.maskedInfo());
        PlanPrices planPrice = planPricesService.getReference(command.planPriceId());
        Subscriptions subscription = subscriptionsService.registerSubscription(
                command.userId(), billingKeys, planPrice, command.plan(), command.amount());
        return new BillingTarget(subscription.getId(), command.userId(), command.provider(),
                command.providerCredential(), command.amount(), false);
    }

    /**
     * 등록 콜백 컨트롤러(Toss/Kakao)가 실제로 호출하는 진입점 - 빌링키/구독 생성 + 가입 직후 첫 청구를
     * 한 흐름으로 묶는다. PG 호출은 트랜잭션 밖에서 한다(SubscriptionChargeCoordinator와 같은 이유).
     * 첫 청구가 성공해야만 OWNER로 승격한다 - 실패하면 등록 자체를 실패 처리(구독 EXPIRED, 빌링키 PG에서도
     * revoke)하고 예외를 던져서 사용자가 바로 재시도하게 한다. PAST_DUE 유예는 안 준다(첫 결제는 재시도
     * 스케줄을 며칠씩 기다릴 이유가 없음).
     */
    public void confirmRegistrationAndCharge(ConfirmRegistrationCommand command) {
        BillingTarget target = confirmRegistration(command);

        PreparedCharge prepared = paymentsService.prepareCharge(target.subscriptionId(), target.amount());
        ChargeResult result = gatewayRegistry.getGateway(target.provider())
                .charge(target.providerCredential(), target.amount(), prepared.orderId());

        if (result.success()) {
            subscriptionChargeService.recordInitialChargeSuccess(
                    prepared.paymentId(), result.providerTransactionId(), result.payToken(), result.rawResponse());
            roleChangeEventPublisher.requestRoleChange(command.userId(), RoleChangeRequested.OWNER, command.tokenId());
            return;
        }

        subscriptionChargeService.recordInitialChargeFailure(
                prepared.paymentId(), target.subscriptionId(), result.failureReason(), result.rawResponse());
        gatewayRegistry.getGateway(target.provider()).revoke(target.providerCredential());
        billingKeysService.findActiveByUserId(command.userId())
                .ifPresent(billingKeys -> billingKeysService.deleteBillingKeys(billingKeys.getId()));
        throw new InitialChargeFailedException(result.failureReason());
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
     * 결제수단 변경 시작 전 확인. PG(Toss/Kakao)는 같은 userId로 이미 활성 빌링키가 있으면
     * 새 빌링키 발급 자체를 거부한다(BILLING_KEY_ALREADY_ACTIVATED) - 그래서 같은 PG로는 절대
     * 바꿀 수 없고, 지금 활성 PG와 "다른" PG로만 변경을 허용한다(Toss<->Kakao만 가능).
     */
    public BillingKeys verifyEligibleForBillingKeyChange(Long userId, Provider newProvider) {
        BillingKeys active = billingKeysService.findActiveByUserId(userId)
                .orElseThrow(() -> new NotFoundBillingKeysException("user=" + userId + "의 활성 빌링키를 찾을 수 없습니다."));
        if (active.getProvider() == newProvider) {
            throw new SameProviderBillingKeyChangeException(newProvider);
        }
        return active;
    }

    /**
     * 청구 직전에 SubscriptionChargeCoordinator가 호출 - 예약(PENDING)된 결제수단 변경이 있으면
     * 그제서야 실제로 스왑하고 새 키로 청구하게 한다. 예약 키는 항상 기존 활성 키와 다른 PG이므로
     * (verifyEligibleForBillingKeyChange가 시작 시점에 이미 보장) 기존 키를 PG에서 revoke해도
     * 새 키한테 영향이 없다 - 예약이 없으면 원래 청구에 쓰려던 provider/credential을 그대로 돌려준다.
     */
    @Transactional
    public ActiveBillingKey applyPendingBillingKeyIfAny(Long userId, Long subscriptionId,
                                                          Provider currentProvider, String currentCredential) {
        return billingKeysService.findPendingByUserId(userId)
                .map(pending -> {
                    billingKeysService.findActiveByUserId(userId).ifPresent(old -> {
                        gatewayRegistry.getGateway(old.getProvider()).revoke(old.getProviderCredential());
                        billingKeysService.deleteBillingKeys(old.getId());
                    });
                    billingKeysService.activateBillingKey(pending.getId());
                    subscriptionsService.replaceBillingKey(subscriptionId, pending);
                    return new ActiveBillingKey(pending.getProvider(), pending.getProviderCredential());
                })
                .orElse(new ActiveBillingKey(currentProvider, currentCredential));
    }

    /**
     * 구독이 Dunning 재시도를 다 소진하고 EXPIRED로 전이된 직후 SubscriptionChargeCoordinator가 호출.
     * 더 이상 청구할 일이 없는 빌링키를 PG에서도 revoke하고 우리 쪽도 DELETED로 정리한다 -
     * 안 하면 이 유저가 나중에 재결제할 때 PG가 "이미 활성 빌링키 있음"으로 새 등록 자체를 거부한다.
     */
    @Transactional
    public void revokeBillingKeyAfterExpiry(Long userId) {
        billingKeysService.findActiveByUserId(userId).ifPresent(billingKeys -> {
            gatewayRegistry.getGateway(billingKeys.getProvider()).revoke(billingKeys.getProviderCredential());
            billingKeysService.deleteBillingKeys(billingKeys.getId());
        });
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
