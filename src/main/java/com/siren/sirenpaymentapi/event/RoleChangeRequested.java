package com.siren.sirenpaymentapi.event;

/**
 * jti는 Account가 role 변경에 맞춰 JWT를 재발급하는 데 씀 - nullable이다.
 * 필드명은 account-api의 PaymentCompleteMessage(userId, role, jti)와 맞춘 것 - 컨슈머 계약.
 */
public record RoleChangeRequested(Long userId, String role, String jti) {
    public static final String OWNER = "OWNER";
    public static final String NORMAL = "NORMAL";
}
