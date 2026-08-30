package com.siren.sirenpaymentapi.elasticsearch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MailFailureLogServiceTest {

    @Mock
    private MailFailureRepository mailFailureRepository;

    @InjectMocks
    private MailFailureLogService mailFailureLogService;

    @Test
    void saveStoresDocument() {
        mailFailureLogService.save(1L, "PAY_SUCCESS", "SMTP 실패");

        verify(mailFailureRepository).save(any(MailFailureDocument.class));
    }

    @Test
    void saveDoesNotThrowWhenRepositoryFails() {
        when(mailFailureRepository.save(any())).thenThrow(new RuntimeException("ES 장애"));

        assertDoesNotThrow(() -> mailFailureLogService.save(1L, "PAY_SUCCESS", "SMTP 실패"));
    }
}
