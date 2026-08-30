package com.siren.sirenpaymentapi.mail.impl;

import com.siren.sirenpaymentapi.dto.mail.SubEndedMailContext;
import com.siren.sirenpaymentapi.mail.MailCategory;
import com.siren.sirenpaymentapi.mail.MailTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SubEndedTemplate implements MailTemplate<SubEndedMailContext> {
    @Override
    public MailCategory getMailCategory() {
        return MailCategory.ENDED;
    }

    @Override
    public Optional<String> getMailContent(SubEndedMailContext context) {
        String htmlContent = """
            <div style="font-family: 'Apple SD Gothic Neo', 'Noto Sans KR', sans-serif; max-width: 600px; margin: 0 auto; padding: 40px 20px; background-color: #f9f9f9;">
                <div style="background-color: #ffffff; padding: 40px; border-radius: 12px; box-shadow: 0 4px 6px rgba(0,0,0,0.05); text-align: center;">
                    <h1 style="color: #333333; font-size: 24px; margin-bottom: 20px;">구독 종료 안내</h1>
                    <p style="color: #666666; font-size: 16px; line-height: 1.6; margin-bottom: 30px;">
                        안녕하세요.<br><strong>%s</strong> 플랜 이용 기간이 종료되어 구독이 마무리되었습니다.
                    </p>
                    <div style="background-color: #f0f4f8; border: 1px solid #dce4ec; border-radius: 8px; padding: 20px; margin-bottom: 30px; text-align: left;">
                        <p style="margin: 0; color: #333333; font-size: 15px;">종료 일시&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;%s</p>
                    </div>
                    <p style="color: #999999; font-size: 14px; font-weight: 500; margin-top: 20px;">
                        그동안 4iren을 이용해 주셔서 감사합니다.
                    </p>
                    <div style="margin-top: 40px; padding-top: 20px; border-top: 1px solid #eeeeee;">
                        <p style="color: #999999; font-size: 12px; margin: 0;">
                            본 메일은 발신 전용이며, 회신되지 않습니다.<br>
                            © 2026 4iren. All rights reserved.
                        </p>
                    </div>
                </div>
            </div>
            """.formatted(context.plan(), context.endedAt());
        return Optional.of(htmlContent);
    }
}
