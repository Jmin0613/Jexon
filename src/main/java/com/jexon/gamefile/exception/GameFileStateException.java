package com.jexon.gamefile.exception;

public class GameFileStateException extends RuntimeException {
    public GameFileStateException() {
        super("공개 중인 게임 버전의 파일 정보를 찾을 수 없습니다.");
    }
}
