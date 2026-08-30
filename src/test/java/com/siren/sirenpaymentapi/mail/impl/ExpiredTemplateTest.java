package com.siren.sirenpaymentapi.mail.impl;

import com.siren.sirenpaymentapi.dto.mail.ExpiredMailContext;
import com.siren.sirenpaymentapi.mail.MailCategory;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ExpiredTemplateTest {

    private final ExpiredTemplate template = new ExpiredTemplate();

    @Test
    void categoryIsExpired() {
        assertEquals(MailCategory.EXPIRED, template.getMailCategory());
    }

    @Test
    void contentIncludesContextFields() {
        ExpiredMailContext context = new ExpiredMailContext(
                "YEARLY", LocalDateTime.of(2026, 8, 28, 10, 0), "재시도 3회 모두 실패");

        String html = template.getMailContent(context).orElseThrow();

        assertTrue(html.contains("YEARLY"));
        assertTrue(html.contains(context.expiredAt().toString()));
        assertTrue(html.contains("재시도 3회 모두 실패"));
    }
}
