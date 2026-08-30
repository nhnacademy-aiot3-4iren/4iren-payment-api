package com.siren.sirenpaymentapi.dto.mail;

import com.siren.sirenpaymentapi.mail.MailCategory;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PaymentSuccessMailContext(
        String plan,
        Long amount,
        LocalDateTime approvedAt,
        String maskedInfo,
        LocalDate nextBillingDate
) implements MailContext {
    @Override
    public MailCategory getMailCategory() {
        return MailCategory.PAY_SUCCESS;
    }
}
