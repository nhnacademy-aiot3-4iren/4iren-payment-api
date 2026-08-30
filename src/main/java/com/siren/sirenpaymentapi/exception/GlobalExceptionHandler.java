package com.siren.sirenpaymentapi.exception;

import com.siren.sirenpaymentapi.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// 상태코드 매핑 근거는 Obsidian 아키텍처-Q&A 참고
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler({
            NotFoundBillingKeysException.class,
            NotFoundPaymentsException.class,
            NotFoundPlanPriceException.class,
            NotFoundSubscriptionException.class
    })
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException e) {
        return respond(HttpStatus.NOT_FOUND, e);
    }

    @ExceptionHandler(BillingKeyCallbackException.class)
    public ResponseEntity<ErrorResponse> handleBadCallback(BillingKeyCallbackException e) {
        return respond(HttpStatus.BAD_REQUEST, e);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException e) {
        return respond(HttpStatus.FORBIDDEN, e);
    }

    @ExceptionHandler(InvalidRoleException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRole(InvalidRoleException e) {
        return respond(HttpStatus.UNAUTHORIZED, e);
    }

    @ExceptionHandler({
            AlreadyBelongsToTeamException.class,
            SameProviderBillingKeyChangeException.class
    })
    public ResponseEntity<ErrorResponse> handleConflict(RuntimeException e) {
        return respond(HttpStatus.CONFLICT, e);
    }

    @ExceptionHandler({
            BillingKeyRegistrationException.class,
            BillingKeyRemoveException.class,
            InactiveBillingKeyException.class,
            InitialChargeFailedException.class
    })
    public ResponseEntity<ErrorResponse> handlePgFailure(RuntimeException e) {
        log.error("PG 연동 실패", e);
        return respond(HttpStatus.BAD_GATEWAY, e);
    }

    @ExceptionHandler({
            CoreApiUnavailableException.class,
            AccountApiUnavailableException.class
    })
    public ResponseEntity<ErrorResponse> handleServiceUnavailable(RuntimeException e) {
        return respond(HttpStatus.SERVICE_UNAVAILABLE, e);
    }

    @ExceptionHandler(JsonConversionException.class)
    public ResponseEntity<ErrorResponse> handleInternal(JsonConversionException e) {
        log.error("내부 오류", e);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, e);
    }

    private ResponseEntity<ErrorResponse> respond(HttpStatus status, Exception e) {
        return ResponseEntity.status(status).body(new ErrorResponse(status.value(), e.getMessage()));
    }
}
