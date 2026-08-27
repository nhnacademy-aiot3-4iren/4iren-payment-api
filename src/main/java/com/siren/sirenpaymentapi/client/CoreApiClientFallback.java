package com.siren.sirenpaymentapi.client;

import com.siren.sirenpaymentapi.dto.core.TeamCheckRequest;
import com.siren.sirenpaymentapi.dto.core.TeamCheckResponse;
import com.siren.sirenpaymentapi.exception.CoreApiUnavailableException;
import org.springframework.stereotype.Component;

// Core를 못 부르면(장애/타임아웃) 팀 소속 여부를 확인할 수 없으므로, 빈 배열 등으로 조용히 통과시키지 않고
// 예외를 던져서 등록 자체를 막는다(fail-closed).
@Component
public class CoreApiClientFallback implements CoreApiClient {

    @Override
    public TeamCheckResponse checkTeams(TeamCheckRequest request) {
        throw new CoreApiUnavailableException(request.userId());
    }
}
