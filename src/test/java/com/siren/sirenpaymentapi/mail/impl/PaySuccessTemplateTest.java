package com.siren.sirenpaymentapi.mail.impl;

import com.siren.sirenpaymentapi.dto.mail.PaymentSuccessMailContext;
import com.siren.sirenpaymentapi.mail.MailCategory;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class PaySuccessTemplateTest {

    private final PaySuccessTemplate template = new PaySuccessTemplate();

    @Test
    void categoryIsPaySuccess() {
        assertEquals(MailCategory.PAY_SUCCESS, template.getMailCategory());
    }

    @Test
    void contentIncludesContextFields() {
        PaymentSuccessMailContext context = new PaymentSuccessMailContext(
                "MONTHLY", 29000L, LocalDateTime.of(2026, 8, 28, 10, 0), "토스페이", LocalDate.of(2026, 9, 28));

        String html = template.getMailContent(context).orElseThrow();

        assertTrue(html.contains("MONTHLY"));
        assertTrue(html.contains("29,000원"));
        assertTrue(html.contains("토스페이"));
        assertTrue(html.contains(context.nextBillingDate().toString()));
        assertTrue(html.contains(context.approvedAt().toString()));
    }
}
