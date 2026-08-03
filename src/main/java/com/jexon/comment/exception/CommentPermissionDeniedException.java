package com.jexon.comment.exception;

public class CommentPermissionDeniedException extends RuntimeException {
    public CommentPermissionDeniedException(String message) {
        super(message);
    }
}
