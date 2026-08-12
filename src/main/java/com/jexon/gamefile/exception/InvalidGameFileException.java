package com.jexon.gamefile.exception;

public class InvalidGameFileException extends RuntimeException {
    public InvalidGameFileException(String message) {
        super(message);
    }

    public InvalidGameFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
