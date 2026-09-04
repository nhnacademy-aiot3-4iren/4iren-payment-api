package com.siren.sirenpaymentapi.mail;

import com.siren.sirenpaymentapi.dto.mail.MailContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
public class MailTemplateRegistry {
    private final Map<MailCategory, MailTemplate<?>> templates;

    public MailTemplateRegistry(List<MailTemplate<?>> templates) {
        this.templates = templates.stream()
                .collect(Collectors.toMap(MailTemplate::getMailCategory, Function.identity()));
    }

    @SuppressWarnings("unchecked") // category <-> context 타입 대응하는 걸 보장함 (각 템플릿 구현이 자기 카테고리에 맞는 컨텍스트만 선언함)
    public String dispatch(MailContext context) {
        MailTemplate<MailContext> template = (MailTemplate<MailContext>) templates.get(context.getMailCategory());
        if(template == null) {
            // 개발 실수일 경우.
            log.error("[MailTemplateRegistry] MailCategory={}에 대응하는 템플릿 빈 없음", context.getMailCategory());
            return null;
        }
        return template.getMailContent(context).orElse(null);
    }
}
