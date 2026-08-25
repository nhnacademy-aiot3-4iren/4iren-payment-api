package com.siren.sirenpaymentapi.service.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.siren.sirenpaymentapi.exception.JsonConversionException;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * PG 인증 대기 등, 비동기 콜백이 나중에 돌아올 때 원요청과 상관관계를 맞추기 위한 Redis TTL 캐시 공통 부모.
 */
public abstract class AbstractRedisTtlCache<K, T> {
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Class<T> valueType;

    protected AbstractRedisTtlCache(StringRedisTemplate redisTemplate, ObjectMapper objectMapper, Class<T> valueType) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.valueType = valueType;
    }

    protected abstract String prefix();

    protected abstract Duration ttl();

    public void save(K key, T value) {
        redisTemplate.opsForValue().set(buildKey(key), writeJson(value), ttl());
    }

    /**
     * 조회와 동시에 삭제 - 콜백이 재시도로 두 번 와도 두 번째는 항상 비어있게.
     * 값이 없으면(TTL 만료 또는 이미 소비됨) Optional.empty() - 호출부가 멱등하게 처리해야 한다.
     */
    public Optional<T> consume(K key) {
        String json = redisTemplate.opsForValue().getAndDelete(buildKey(key));
        return json == null ? Optional.empty() : Optional.of(readJson(json));
    }

    private String buildKey(K key) {
        Objects.requireNonNull(key, "key는 null일 수 없습니다.");
        return prefix() + key;
    }

    private String writeJson(T value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new JsonConversionException(prefix() + " JSON 직렬화 실패", e);
        }
    }

    private T readJson(String json) {
        try {
            return objectMapper.readValue(json, valueType);
        } catch (JsonProcessingException e) {
            throw new JsonConversionException(prefix() + " JSON 파싱 실패", e);
        }
    }
}
