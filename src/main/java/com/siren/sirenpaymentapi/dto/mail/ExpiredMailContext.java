package com.siren.sirenpaymentapi.dto.mail;

import com.siren.sirenpaymentapi.mail.MailCategory;

import java.time.LocalDateTime;

public record ExpiredMailContext(
        String plan,
        LocalDateTime expiredAt,
        String failureReason
) implements MailContext {
    @Override
    public MailCategory getMailCategory() {
        return MailCategory.EXPIRED;
    }
}
