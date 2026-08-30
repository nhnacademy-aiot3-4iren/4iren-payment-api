package com.siren.sirenpaymentapi.mail.impl;

import com.siren.sirenpaymentapi.dto.mail.CanceledMailContext;
import com.siren.sirenpaymentapi.mail.MailCategory;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CanceledTemplateTest {

    private final CanceledTemplate template = new CanceledTemplate();

    @Test
    void categoryIsCanceled() {
        assertEquals(MailCategory.CANCELED, template.getMailCategory());
    }

    @Test
    void contentIncludesContextFields() {
        CanceledMailContext context = new CanceledMailContext(
                "MONTHLY", LocalDateTime.of(2026, 8, 28, 10, 0), LocalDateTime.of(2026, 9, 28, 10, 0));

        String html = template.getMailContent(context).orElseThrow();

        assertTrue(html.contains("MONTHLY"));
        assertTrue(html.contains(context.canceledAt().toString()));
        assertTrue(html.contains(context.currentPeriodEnd().toString()));
    }
}
