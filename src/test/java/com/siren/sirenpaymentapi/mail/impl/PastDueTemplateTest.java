package com.siren.sirenpaymentapi.mail.impl;

import com.siren.sirenpaymentapi.dto.mail.PastDueMailContext;
import com.siren.sirenpaymentapi.mail.MailCategory;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class PastDueTemplateTest {

    private final PastDueTemplate template = new PastDueTemplate();

    @Test
    void categoryIsPastDue() {
        assertEquals(MailCategory.PAST_DUE, template.getMailCategory());
    }

    @Test
    void contentIncludesContextFields() {
        PastDueMailContext context = new PastDueMailContext(
                "MONTHLY", LocalDateTime.of(2026, 8, 28, 10, 0), LocalDate.of(2026, 8, 29),
                1, 3, "카드 한도 초과");

        String html = template.getMailContent(context).orElseThrow();

        assertTrue(html.contains("MONTHLY"));
        assertTrue(html.contains(context.nextRetryDate().toString()));
        assertTrue(html.contains("1 / 3회"));
        assertTrue(html.contains("카드 한도 초과"));
    }
}
