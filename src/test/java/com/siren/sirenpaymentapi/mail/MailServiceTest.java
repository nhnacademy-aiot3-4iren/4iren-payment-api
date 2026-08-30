package com.siren.sirenpaymentapi.mail;

import com.siren.sirenpaymentapi.client.AccountApiClient;
import com.siren.sirenpaymentapi.dto.mail.MailContext;
import com.siren.sirenpaymentapi.dto.mail.PaymentSuccessMailContext;
import com.siren.sirenpaymentapi.exception.MailSendFailedException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MailTemplateRegistry registry;

    @Mock
    private AccountApiClient accountApiClient;

    private MailService mailService;

    private final MailContext context = new PaymentSuccessMailContext(
            "MONTHLY", 29000L, LocalDateTime.now(), "토스페이", LocalDate.now());

    @BeforeEach
    void setUp() {
        RetryTemplate retryTemplate = new RetryTemplate();
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(3);
        retryTemplate.setRetryPolicy(retryPolicy);

        mailService = new MailService(mailSender, registry, retryTemplate, accountApiClient);
        ReflectionTestUtils.setField(mailService, "fromEmail", "noreply@4iren.site");
    }

    @Test
    void skipsWhenFromEmailBlank() {
        ReflectionTestUtils.setField(mailService, "fromEmail", "");

        mailService.sendMail(1L, context);

        verifyNoInteractions(registry, mailSender, accountApiClient);
    }

    @Test
    void skipsWhenTemplateContentMissing() {
        when(registry.dispatch(context)).thenReturn(null);

        mailService.sendMail(1L, context);

        verifyNoInteractions(mailSender, accountApiClient);
    }

    @Test
    void sendsMailSuccessfully() {
        when(registry.dispatch(context)).thenReturn("<html>ok</html>");
        when(accountApiClient.getEmail(1L)).thenReturn("user@example.com");
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        mailService.sendMail(1L, context);

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void throwsMailSendFailedExceptionWhenSendKeepsFailing() {
        when(registry.dispatch(context)).thenReturn("<html>ok</html>");
        when(accountApiClient.getEmail(1L)).thenReturn("user@example.com");
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("SMTP 연결 실패"));

        assertThrows(MailSendFailedException.class, () -> mailService.sendMail(1L, context));

        verify(accountApiClient, times(3)).getEmail(1L); // maxAttempts=3
    }

    @Test
    void throwsMailSendFailedExceptionWhenEmailLookupKeepsFailing() {
        when(registry.dispatch(context)).thenReturn("<html>ok</html>");
        when(accountApiClient.getEmail(1L)).thenThrow(new RuntimeException("Account API 장애"));

        assertThrows(MailSendFailedException.class, () -> mailService.sendMail(1L, context));

        verifyNoInteractions(mailSender);
    }
}
