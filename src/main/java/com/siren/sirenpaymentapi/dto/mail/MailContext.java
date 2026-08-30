package com.siren.sirenpaymentapi.dto.mail;

import com.siren.sirenpaymentapi.mail.MailCategory;

public sealed interface MailContext permits
        PaymentSuccessMailContext,
        PastDueMailContext,
        ExpiredMailContext,
        CanceledMailContext,
        SubEndedMailContext{

    MailCategory getMailCategory();
}
