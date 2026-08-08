package com.jexon.gameversion.exception;

public class GameVersionConcurrencyConflictException extends RuntimeException {
    public GameVersionConcurrencyConflictException() {
        super("다른 관리자가 게임 버전 정보를 변경했습니다. 최신 상태를 확인한 후 다시 시도해주세요.");
    }
}
