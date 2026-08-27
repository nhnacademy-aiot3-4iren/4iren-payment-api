package com.siren.sirenpaymentapi.service.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siren.sirenpaymentapi.dto.kakao.PendingRegistration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class KakaoPendingRegistrationCache extends AbstractRedisTtlCache<String, PendingRegistration> {
    private static final String PREFIX = "billing-key-registration:kakao:";
    private static final Duration TTL = Duration.ofMinutes(30);

    public KakaoPendingRegistrationCache(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
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
