package com.siren.sirenpaymentapi.event;

/**
 * tokenId는 Account가 role 변경에 맞춰 JWT를 재발급하는 데 씀 - nullable이다.
 */
public record RoleChangeRequested(Long userId, String targetRole, String tokenId) {
    public static final String OWNER = "OWNER";
    public static final String NORMAL = "NORMAL";
}
