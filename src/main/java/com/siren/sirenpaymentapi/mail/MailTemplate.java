package com.siren.sirenpaymentapi.mail;

import com.siren.sirenpaymentapi.dto.mail.MailContext;

import java.util.Optional;

public interface MailTemplate<T extends MailContext> {
    MailCategory getMailCategory();
    Optional<String> getMailContent(T context);
}
