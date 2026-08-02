package com.jexon.post.exception;

public class PostPermissionDeniedException extends RuntimeException {
    public PostPermissionDeniedException(String message) {
        super(message);
    }
}
