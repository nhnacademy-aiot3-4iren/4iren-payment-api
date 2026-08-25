package com.siren.sirenpaymentapi.client;

import com.siren.sirenpaymentapi.dto.core.TeamCheckRequest;
import com.siren.sirenpaymentapi.dto.core.TeamCheckResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Core의 내부 전용 API 호출용
 */
@FeignClient(name = "4IREN-CORE", contextId = "coreApi", fallback = CoreApiClientFallback.class)
public interface CoreApiClient {

    // 유저가 팀에 속해 있는지 확인용
    @PostMapping("/api/core/internal/users/teams")
    TeamCheckResponse checkTeams(@RequestBody TeamCheckRequest request);
}
