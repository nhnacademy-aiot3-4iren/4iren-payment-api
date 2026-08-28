package com.siren.sirenpaymentapi.controller;

import com.siren.sirenpaymentapi.dto.payments.PaymentHistoryResponse;
import com.siren.sirenpaymentapi.security.RequireRole;
import com.siren.sirenpaymentapi.security.Role;
import com.siren.sirenpaymentapi.service.basic_service.PaymentsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/payment/payments")
@RequiredArgsConstructor
@RequireRole(value = Role.OWNER)
public class PaymentHistoryController {
    private final PaymentsService paymentsService;

    @GetMapping
    public List<PaymentHistoryResponse> getMyPayments(@RequestHeader("X-USER-ID") Long userId) {
        return paymentsService.findByUserId(userId);
    }
}
