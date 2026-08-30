package com.siren.sirenpaymentapi.exception;

import com.siren.sirenpaymentapi.dto.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleNotFoundReturns404() {
        ResponseEntity<ErrorResponse> response = handler.handleNotFound(new NotFoundSubscriptionException(1L));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().status());
    }

    @Test
    void handleBadCallbackReturns400() {
        ResponseEntity<ErrorResponse> response = handler.handleBadCallback(
                new BillingKeyCallbackException(com.siren.sirenpaymentapi.domain.Provider.TOSS_PAY, "REMOVED"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void handleConflictReturns409() {
        ResponseEntity<ErrorResponse> response = handler.handleConflict(new AlreadyBelongsToTeamException(1L));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void handleConflictReturns409ForSameProviderChange() {
        ResponseEntity<ErrorResponse> response = handler.handleConflict(
                new SameProviderBillingKeyChangeException(com.siren.sirenpaymentapi.domain.Provider.TOSS_PAY));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void handlePgFailureReturns502() {
        ResponseEntity<ErrorResponse> response = handler.handlePgFailure(new BillingKeyRemoveException("실패"));

        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
    }

    @Test
    void handleServiceUnavailableReturns503() {
        ResponseEntity<ErrorResponse> response = handler.handleServiceUnavailable(new CoreApiUnavailableException(1L));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
    }

    @Test
    void handleServiceUnavailableReturns503ForAccountApi() {
        ResponseEntity<ErrorResponse> response = handler.handleServiceUnavailable(new AccountApiUnavailableException(1L));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
    }

    @Test
    void handleInternalReturns500() {
        ResponseEntity<ErrorResponse> response = handler.handleInternal(
                new JsonConversionException("파싱 실패", new RuntimeException()));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void handleForbiddenReturns403() {
        ResponseEntity<ErrorResponse> response = handler.handleForbidden(new ForbiddenException("접근 권한이 없습니다."));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void handleInvalidRoleReturns401() {
        ResponseEntity<ErrorResponse> response = handler.handleInvalidRole(
                new InvalidRoleException("X-USER-ROLE 헤더가 없습니다."));

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
}
