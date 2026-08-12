package com.jexon.gamefile.exception;

public class DuplicateGameFileException extends RuntimeException {
    public DuplicateGameFileException() {
        super("이미 게임 파일이 등록된 게임 버전입니다.");
    }
}
