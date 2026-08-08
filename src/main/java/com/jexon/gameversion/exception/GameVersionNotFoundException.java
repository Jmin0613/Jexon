package com.jexon.gameversion.exception;

public class GameVersionNotFoundException extends RuntimeException {
    public GameVersionNotFoundException() {
        super("게임 버전을 찾을 수 없습니다.");
    }
}
