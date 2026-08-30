package com.siren.sirenpaymentapi.dto.mail;

import com.siren.sirenpaymentapi.mail.MailCategory;

import java.time.LocalDateTime;

public record SubEndedMailContext(
        String plan,
        LocalDateTime endedAt
) implements MailContext{
    @Override
    public MailCategory getMailCategory() {
        return MailCategory.ENDED;
    }
}
