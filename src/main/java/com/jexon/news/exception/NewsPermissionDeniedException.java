package com.jexon.news.exception;

public class NewsPermissionDeniedException extends RuntimeException {
    public NewsPermissionDeniedException(String message) {
        super(message);
    }
}
