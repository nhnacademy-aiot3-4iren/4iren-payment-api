package com.siren.sirenpaymentapi.dto.mail;

import com.siren.sirenpaymentapi.mail.MailCategory;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PastDueMailContext(
        String plan,
        LocalDateTime failedAt,
        LocalDate nextRetryDate,
        int retryCount,
        int maxRetryCount,
        String failureReason
) implements MailContext {
    @Override
    public MailCategory getMailCategory() {
        return MailCategory.PAST_DUE;
    }
}
