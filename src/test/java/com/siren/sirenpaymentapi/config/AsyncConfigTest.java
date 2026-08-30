package com.siren.sirenpaymentapi.config;

import com.siren.sirenpaymentapi.dto.mail.SubEndedMailContext;
import com.siren.sirenpaymentapi.elasticsearch.MailFailureLogService;
import com.siren.sirenpaymentapi.mail.MailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AsyncConfigTest {

    @Mock
    private MailFailureLogService mailFailureLogService;

    @InjectMocks
    private AsyncConfig asyncConfig;

    @Test
    void getAsyncExecutorReturnsUsableExecutor() {
        Executor executor = asyncConfig.getAsyncExecutor();

        assertNotNull(executor);
    }

    @Test
    void uncaughtExceptionHandlerLogsFailureWhenParamsMatchSendMailSignature() throws NoSuchMethodException {
        AsyncUncaughtExceptionHandler handler = asyncConfig.getAsyncUncaughtExceptionHandler();
        Method sendMail = MailService.class.getMethod("sendMail", Long.class, com.siren.sirenpaymentapi.dto.mail.MailContext.class);
        var context = new SubEndedMailContext("MONTHLY", LocalDateTime.now());

        handler.handleUncaughtException(new RuntimeException("SMTP 실패"), sendMail, 1L, context);

        verify(mailFailureLogService).save(1L, "ENDED", "SMTP 실패");
    }

    @Test
    void uncaughtExceptionHandlerDoesNothingWhenParamsDontMatch() throws NoSuchMethodException {
        AsyncUncaughtExceptionHandler handler = asyncConfig.getAsyncUncaughtExceptionHandler();
        Method toString = Object.class.getMethod("toString");

        handler.handleUncaughtException(new RuntimeException("무관한 실패"), toString);

        verifyNoInteractions(mailFailureLogService);
    }
}
