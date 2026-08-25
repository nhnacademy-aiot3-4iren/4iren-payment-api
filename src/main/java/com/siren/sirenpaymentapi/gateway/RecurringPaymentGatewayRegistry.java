package com.siren.sirenpaymentapi.gateway;

import com.siren.sirenpaymentapi.domain.Provider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Provider(TOSS/KAKAO/NAVER)에 맞는 RecurringPaymentGateway 구현체를 찾아주는 창구.
 * Spring이 등록된 모든 RecurringPaymentGateway 빈을 리스트로 주입해주면, 그걸 Provider 기준으로 맵핑해둔다.
 */
@Component
public class RecurringPaymentGatewayRegistry {

    private final Map<Provider, RecurringPaymentGateway> gatewaysByProvider;

    public RecurringPaymentGatewayRegistry(List<RecurringPaymentGateway> gateways) {
        this.gatewaysByProvider = gateways.stream()
                .collect(Collectors.toUnmodifiableMap(RecurringPaymentGateway::getProvider, Function.identity()));
    }

    public RecurringPaymentGateway getGateway(Provider provider) {
        RecurringPaymentGateway gateway = gatewaysByProvider.get(provider);
        if (gateway == null) {
            throw new IllegalArgumentException("지원하지 않는 결제수단: " + provider);
        }
        return gateway;
    }
}
