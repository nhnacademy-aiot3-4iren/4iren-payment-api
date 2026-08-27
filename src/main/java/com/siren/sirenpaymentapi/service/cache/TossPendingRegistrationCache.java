package com.siren.sirenpaymentapi.service.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siren.sirenpaymentapi.dto.toss.PendingRegistration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class TossPendingRegistrationCache extends AbstractRedisTtlCache<String, PendingRegistration> {
    // PG별로 네임스페이스 분리(기존엔 그냥 "billing-key-registration:"이었음 - 카카오 캐시 추가하면서 분리)
    private static final String PREFIX = "billing-key-registration:toss:";
    private static final Duration TTL = Duration.ofMinutes(30);

    public TossPendingRegistrationCache(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        super(redisTemplate, objectMapper, PendingRegistration.class);
    }

    @Override
    protected String prefix() {
        return PREFIX;
    }

    @Override
    protected Duration ttl() {
        return TTL;
    }
}
