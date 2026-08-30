package com.siren.sirenpaymentapi.mail;

import com.siren.sirenpaymentapi.dto.mail.MailContext;
import com.siren.sirenpaymentapi.dto.mail.SubEndedMailContext;
import com.siren.sirenpaymentapi.event.MailNotificationRequested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MailEventPublisherTest {

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private MailService mailService;

    @InjectMocks
    private MailEventPublisher mailEventPublisher;

    private final MailContext context = new SubEndedMailContext("MONTHLY", LocalDateTime.now());

    @Test
    void notifyPublishesEvent() {
        mailEventPublisher.notify(1L, context);

        ArgumentCaptor<MailNotificationRequested> captor = ArgumentCaptor.forClass(MailNotificationRequested.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());
        assertEquals(1L, captor.getValue().userId());
        assertEquals(context, captor.getValue().context());
    }

    @Test
    void onMailNotificationRequestedCallsMailService() {
        MailNotificationRequested event = new MailNotificationRequested(1L, context);

        mailEventPublisher.onMailNotificationRequested(event);

        verify(mailService).sendMail(1L, context);
    }
}
