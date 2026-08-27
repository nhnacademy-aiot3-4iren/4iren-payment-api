package com.siren.sirenpaymentapi.dto.payments;

import com.siren.sirenpaymentapi.domain.PaymentStatus;

import java.time.LocalDateTime;

public record PaymentHistoryResponse(
        Long id,
        Long amount,
        PaymentStatus status,
        String failureReason,
        LocalDateTime attemptedAt,
        LocalDateTime approvedAt
) {
}
