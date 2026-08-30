package com.siren.sirenpaymentapi.dto.mail;

import com.siren.sirenpaymentapi.mail.MailCategory;

import java.time.LocalDateTime;

public record CanceledMailContext(
        String plan,
        LocalDateTime canceledAt,
        LocalDateTime currentPeriodEnd
) implements MailContext {
    @Override
    public MailCategory getMailCategory() {
        return MailCategory.CANCELED;
    }
}
