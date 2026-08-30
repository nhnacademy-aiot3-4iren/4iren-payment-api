package com.siren.sirenpaymentapi.mail;

import com.siren.sirenpaymentapi.dto.mail.MailContext;
import com.siren.sirenpaymentapi.event.MailNotificationRequested;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class MailEventPublisher {
    private final ApplicationEventPublisher applicationEventPublisher;
    private final MailService mailService;

    public void notify(Long userId, MailContext context) {
        applicationEventPublisher.publishEvent(new MailNotificationRequested(userId, context));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onMailNotificationRequested(MailNotificationRequested event) {
        mailService.sendMail(event.userId(), event.context());
    }
}
