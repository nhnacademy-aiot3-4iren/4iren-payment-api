package com.siren.sirenpaymentapi.service.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siren.sirenpaymentapi.domain.Plan;
import com.siren.sirenpaymentapi.domain.RegistrationMode;
import com.siren.sirenpaymentapi.dto.kakao.PendingRegistration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KakaoPendingRegistrationCacheTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private KakaoPendingRegistrationCache kakaoPendingRegistrationCache;

    @BeforeEach
    void setUp() {
        kakaoPendingRegistrationCache = new KakaoPendingRegistrationCache(redisTemplate, new ObjectMapper());
    }

    @Test
    void saveWritesJsonWithKakaoPrefix() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        PendingRegistration pendingRegistration = new PendingRegistration(1L, Plan.MONTHLY, 29000L, 1L, "tid-1", "token-1", RegistrationMode.NEW);

        kakaoPendingRegistrationCache.save("order-1", pendingRegistration);

        verify(valueOperations).set(eq("billing-key-registration:kakao:order-1"), anyString(), any());
    }
}
