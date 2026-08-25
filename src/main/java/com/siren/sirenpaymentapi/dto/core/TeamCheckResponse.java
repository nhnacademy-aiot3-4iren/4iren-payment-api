package com.siren.sirenpaymentapi.dto.core;

import java.util.List;

// Core POST /api/core/internal/users/teams 응답 - teams가 비어있으면 어떤 팀에도 안 속함
public record TeamCheckResponse(Long userId, List<Long> teams) {
}
