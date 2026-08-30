package com.siren.sirenpaymentapi.service;

import com.siren.sirenpaymentapi.domain.entity.BillingKeys;
import com.siren.sirenpaymentapi.domain.entity.PlanPrices;
import com.siren.sirenpaymentapi.domain.entity.Subscriptions;
import com.siren.sirenpaymentapi.dto.billing_keys.ConfirmRegistrationCommand;
import com.siren.sirenpaymentapi.dto.subscriptions.BillingTarget;
import com.siren.sirenpaymentapi.service.basic_service.BillingKeysService;
import com.siren.sirenpaymentapi.service.basic_service.PlanPricesService;
import com.siren.sirenpaymentapi.service.basic_service.SubscriptionsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * BillingKeyRegistrationService.confirmRegistrationAndCharge가 이 빈을 통해서 confirmRegistration을
 * 호출한다 - 같은 클래스 안에서 this로 호출하면 @Transactional이 Spring AOP 프록시를 안 거쳐서 무시되는
 * self-invocation 문제가 있어서, 트랜잭션 경계를 진짜로 지키려고 아예 클래스를 분리함.
 */
@Service
@RequiredArgsConstructor
public class RegistrationConfirmationService {
    private final BillingKeysService billingKeysService;
    private final SubscriptionsService subscriptionsService;
    private final PlanPricesService planPricesService;

    /**
     * PG 등록(인증) 절차가 성공으로 확정된 시점에 호출
     * 빌링키 생성 + 구독 생성을 한 트랜잭션으로 묶는다 - 최초 가입 흐름의 진입점.
     * planPriceId는 등록 시작 시점에 확정된 가격 row 참조(가격 고정/grandfathering, PendingRegistration에서 옴).
     * OWNER 승격은 여기서 하지 않는다 - BillingKeyRegistrationService.confirmRegistrationAndCharge가
     * 첫 청구 성공을 확인한 뒤에 한다.
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
}
