package com.siren.sirenpaymentapi.mail;

import com.siren.sirenpaymentapi.dto.mail.MailContext;
import com.siren.sirenpaymentapi.dto.mail.PaymentSuccessMailContext;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MailTemplateRegistryTest {

    private final MailContext context = new PaymentSuccessMailContext(
            "MONTHLY", 29000L, LocalDateTime.now(), "토스페이", LocalDate.now());

    @SuppressWarnings("unchecked")
    @Test
    void dispatchReturnsContentFromMatchingTemplate() {
        MailTemplate<MailContext> template = mock(MailTemplate.class);
        when(template.getMailCategory()).thenReturn(MailCategory.PAY_SUCCESS);
        when(template.getMailContent(any())).thenReturn(Optional.of("<html>ok</html>"));
        MailTemplateRegistry registry = new MailTemplateRegistry(List.of(template));

        assertEquals("<html>ok</html>", registry.dispatch(context));
    }

    @Test
    void dispatchReturnsNullWhenNoTemplateRegisteredForCategory() {
        MailTemplateRegistry registry = new MailTemplateRegistry(List.of());

        assertNull(registry.dispatch(context));
    }

    @SuppressWarnings("unchecked")
    @Test
    void dispatchReturnsNullWhenTemplateContentEmpty() {
        MailTemplate<MailContext> template = mock(MailTemplate.class);
        when(template.getMailCategory()).thenReturn(MailCategory.PAY_SUCCESS);
        when(template.getMailContent(any())).thenReturn(Optional.empty());
        MailTemplateRegistry registry = new MailTemplateRegistry(List.of(template));

        assertNull(registry.dispatch(context));
    }
}
