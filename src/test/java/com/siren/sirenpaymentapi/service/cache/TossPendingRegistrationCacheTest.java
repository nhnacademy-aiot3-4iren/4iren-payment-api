package com.siren.sirenpaymentapi.service.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siren.sirenpaymentapi.domain.Plan;
import com.siren.sirenpaymentapi.dto.toss.PendingRegistration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TossPendingRegistrationCacheTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private TossPendingRegistrationCache tossPendingRegistrationCache;

    @BeforeEach
    void setUp() {
        tossPendingRegistrationCache = new TossPendingRegistrationCache(redisTemplate, new ObjectMapper());
    }

    @Test
    void saveWritesJsonWithPrefix() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        PendingRegistration pendingRegistration = new PendingRegistration(1L, Plan.MONTHLY, 29000L, 1L, "token-1");

        tossPendingRegistrationCache.save("billing-key-1", pendingRegistration);

        verify(valueOperations).set(eq("billing-key-registration:toss:billing-key-1"), anyString(), any());
    }

    @Test
    void consumeReturnsValueWhenPresent() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete("billing-key-registration:toss:billing-key-1"))
                .thenReturn("{\"userId\":1,\"plan\":\"MONTHLY\",\"amount\":29000,\"planPriceId\":1,\"tokenId\":\"token-1\"}");

        Optional<PendingRegistration> result = tossPendingRegistrationCache.consume("billing-key-1");

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().userId());
    }

    @Test
    void consumeReturnsEmptyWhenMissing() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete("billing-key-registration:toss:billing-key-1")).thenReturn(null);

        Optional<PendingRegistration> result = tossPendingRegistrationCache.consume("billing-key-1");

        assertTrue(result.isEmpty());
    }

    @Test
    void saveThrowsWhenKeyIsNull() {
        PendingRegistration pendingRegistration = new PendingRegistration(1L, Plan.MONTHLY, 29000L, 1L, "token-1");

        assertThrows(NullPointerException.class,
                () -> tossPendingRegistrationCache.save(null, pendingRegistration));
    }
}
