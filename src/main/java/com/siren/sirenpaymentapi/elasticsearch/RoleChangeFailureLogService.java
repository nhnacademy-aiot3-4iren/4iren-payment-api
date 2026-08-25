package com.siren.sirenpaymentapi.elasticsearch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoleChangeFailureLogService {

    private final RoleChangeFailureRepository roleChangeFailureRepository;

    // ES 저장 자체가 발행 실패 처리 흐름 전체를 죽이면 안 되므로 여기서 삼킴
    public void save(Long userId, String targetRole, String tokenId, String failureStage, String reason) {
        try {
            roleChangeFailureRepository.save(RoleChangeFailureDocument.builder()
                    .id(UUID.randomUUID().toString())
                    .userId(userId)
                    .targetRole(targetRole)
                    .tokenId(tokenId)
                    .failureStage(failureStage)
                    .reason(reason)
                    .occurredAt(Instant.now())
                    .build());
        } catch (Exception e) {
            log.error("[RoleChangeFailureLogService] ES 적재 실패 - userId={}, failureStage={}", userId, failureStage, e);
        }
    }
}
