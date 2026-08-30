package com.siren.sirenpaymentapi.elasticsearch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailFailureLogService {

    private final MailFailureRepository mailFailureRepository;

    // ES 저장 자체가 메일 발송 실패 처리 흐름을 죽이면 안 되므로 여기서 삼킴
    public void save(Long userId, String category, String reason) {
        try {
            mailFailureRepository.save(MailFailureDocument.builder()
                    .id(UUID.randomUUID().toString())
                    .userId(userId)
                    .category(category)
                    .reason(reason)
                    .occurredAt(Instant.now())
                    .build());
        } catch (Exception e) {
            log.error("[MailFailureLogService] ES 적재 실패 - userId={}, category={}", userId, category, e);
        }
    }
}
