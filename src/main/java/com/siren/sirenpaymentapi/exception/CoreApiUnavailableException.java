package com.siren.sirenpaymentapi.exception;

// Core 내부 API 호출 자체가 실패한 경우 - fail-closed(등록 허용 안 함), 근거는 Obsidian 아키텍처-Q&A 참고
public class CoreApiUnavailableException extends RuntimeException {
    public CoreApiUnavailableException(Long userId) {
        super("Core API 응답 실패 - 팀 소속 여부를 확인할 수 없음 (userId=" + userId + ")");
    }
}
