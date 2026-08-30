package com.siren.sirenpaymentapi.event;

import com.siren.sirenpaymentapi.dto.mail.MailContext;

public record MailNotificationRequested(Long userId, MailContext context) {
}
