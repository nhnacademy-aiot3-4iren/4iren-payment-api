package com.siren.sirenpaymentapi.mail.impl;

import com.siren.sirenpaymentapi.dto.mail.SubEndedMailContext;
import com.siren.sirenpaymentapi.mail.MailCategory;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class SubEndedTemplateTest {

    private final SubEndedTemplate template = new SubEndedTemplate();

    @Test
    void categoryIsEnded() {
        assertEquals(MailCategory.ENDED, template.getMailCategory());
    }

    @Test
    void contentIncludesContextFields() {
        SubEndedMailContext context = new SubEndedMailContext("YEARLY", LocalDateTime.of(2026, 8, 28, 10, 0));

        String html = template.getMailContent(context).orElseThrow();

        assertTrue(html.contains("YEARLY"));
        assertTrue(html.contains(context.endedAt().toString()));
    }
}
