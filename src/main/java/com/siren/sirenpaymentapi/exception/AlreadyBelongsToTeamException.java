package com.siren.sirenpaymentapi.exception;

// 첫결제인데 이미 어떤 팀에든(OWNER든 MEMBER든) 속해있는 유저가 등록을 시도한 경우 - 상태코드는 GlobalExceptionHandler에서 매핑
public class AlreadyBelongsToTeamException extends RuntimeException {
    public AlreadyBelongsToTeamException(Long userId) {
        super("이미 팀에 속해있는 유저는 신규 정기결제를 시작할 수 없습니다 (userId=" + userId + ")");
    }
}
