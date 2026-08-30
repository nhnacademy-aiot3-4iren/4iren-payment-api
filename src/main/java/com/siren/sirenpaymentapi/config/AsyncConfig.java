package com.siren.sirenpaymentapi.config;

import com.siren.sirenpaymentapi.dto.mail.MailContext;
import com.siren.sirenpaymentapi.elasticsearch.MailFailureLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

@Slf4j
@Configuration
@EnableAsync
@RequiredArgsConstructor
public class AsyncConfig implements AsyncConfigurer {
    private final MailFailureLogService mailFailureLogService;

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("mail-async-");
        executor.initialize();
        return executor;
    }

    // MailService.sendMail이 RetryTemplate까지 다 소진하고 MailSendFailedException을 던지면 여기로 옴
    // (@Async void 메서드는 호출부가 이 예외를 못 잡으므로 실패 기록은 반드시 여기서 처리해야 함)
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (Throwable ex, Method method, Object... params) -> {
            log.error("[AsyncUncaughtExceptionHandler] 비동기 메서드 실행 실패 - method={}", method.getName(), ex);
            if (params.length >= 2 && params[0] instanceof Long userId && params[1] instanceof MailContext context) {
                mailFailureLogService.save(userId, context.getMailCategory().name(), ex.getMessage());
            }
        };
    }
}
