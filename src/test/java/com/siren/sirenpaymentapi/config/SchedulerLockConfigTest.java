package com.siren.sirenpaymentapi.config;

import net.javacrumbs.shedlock.core.LockProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SchedulerLockConfigTest {

    @Mock
    private RedisConnectionFactory redisConnectionFactory;

    private final SchedulerLockConfig schedulerLockConfig = new SchedulerLockConfig();

    @Test
    void lockProviderIsNotNull() {
        LockProvider lockProvider = schedulerLockConfig.lockProvider(redisConnectionFactory);

        assertNotNull(lockProvider);
    }
}
