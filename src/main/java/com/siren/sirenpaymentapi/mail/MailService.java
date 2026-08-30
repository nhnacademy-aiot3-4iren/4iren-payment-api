package com.siren.sirenpaymentapi.mail;

import com.siren.sirenpaymentapi.client.AccountApiClient;
import com.siren.sirenpaymentapi.dto.mail.MailContext;
import com.siren.sirenpaymentapi.exception.MailSendFailedException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {
    private final JavaMailSender mailSender;
    private final MailTemplateRegistry registry;
    private final RetryTemplate retryTemplate; // RabbitMQConfig에 이미 있는 빈 재사용(최대 3회, 지수 백오프)
    private final AccountApiClient accountApiClient;

    @Value("${sender.address:${spring.sender.address:${mail.sender.address:${spring.mail.username:}}}}")
    private String fromEmail;

    // 호출부는 userId만 알면 됨 - 이메일 조회(AccountApiClient)까지 여기서 책임짐
    @Async
    public void sendMail(Long userId, MailContext context){
        if(fromEmail == null || fromEmail.isBlank()){
            log.warn("[MailService] 발신자 설정안됨 => 메일 전송 불가능");
            return;
        }

        MailCategory category = context.getMailCategory();
        String htmlContent = registry.dispatch(context);
        if (htmlContent == null) {
            log.warn("[MailService] MailCategory={} 컨텐츠 없음 - 발송 스킵", category);
            return;
        }

        try{
            retryTemplate.execute(retryContext -> {
                // 이메일 조회도 재시도 범위 안에 둠 - Account API가 일시적으로 흔들려도 같은 예산(3회)으로 커버
                String toEmail = accountApiClient.getEmail(userId);
                log.debug("email: {}", toEmail);
                MimeMessage mimeMessage = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");

                helper.setFrom(fromEmail);
                helper.setTo(toEmail);
                helper.setSubject("[4iren] " + category.getSubject());
                helper.setText(htmlContent, true);

                mailSender.send(mimeMessage);
                return null;
            });
            log.info("[MailService] MailCategory: {} 이메일 전송 완료 - userId={}", category, userId);
        }catch (Exception e){
            log.error("[MailService] 이메일 전송 실패 - userId={}", userId, e);
            throw new MailSendFailedException("메일 전송에 실패했습니다."+ e.getMessage());
        }
    }
}
