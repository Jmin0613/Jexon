package com.jexon.member.exception;

public class MemberPermissionDeniedException extends RuntimeException {
    public MemberPermissionDeniedException(String message) { super(message); }
}
