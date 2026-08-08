package com.jexon.gameversion.exception;

public class DuplicateGameVersionException extends RuntimeException {
    public DuplicateGameVersionException() {
        super("이미 등록된 게임 버전입니다.");
    }
}
