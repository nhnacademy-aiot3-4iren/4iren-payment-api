package com.siren.sirenpaymentapi.elasticsearch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleChangeFailureLogServiceTest {

    @Mock
    private RoleChangeFailureRepository roleChangeFailureRepository;

    @InjectMocks
    private RoleChangeFailureLogService roleChangeFailureLogService;

    @Test
    void saveStoresDocument() {
        roleChangeFailureLogService.save(1L, "OWNER", "token-1", "ROUTING_FAILED", "NO_ROUTE");

        verify(roleChangeFailureRepository).save(any(RoleChangeFailureDocument.class));
    }

    @Test
    void saveDoesNotThrowWhenRepositoryFails() {
        when(roleChangeFailureRepository.save(any())).thenThrow(new RuntimeException("ES 장애"));

        assertDoesNotThrow(() ->
                roleChangeFailureLogService.save(1L, "OWNER", "token-1", "ROUTING_FAILED", "NO_ROUTE"));
    }
}
