package com.siren.sirenpaymentapi.client;

import com.siren.sirenpaymentapi.dto.core.TeamCheckRequest;
import com.siren.sirenpaymentapi.exception.CoreApiUnavailableException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CoreApiClientFallbackTest {

    private final CoreApiClientFallback coreApiClientFallback = new CoreApiClientFallback();

    @Test
    void checkTeamsThrowsCoreApiUnavailableException() {
        TeamCheckRequest request = new TeamCheckRequest(1L);

        assertThrows(CoreApiUnavailableException.class, () -> coreApiClientFallback.checkTeams(request));
    }
}
