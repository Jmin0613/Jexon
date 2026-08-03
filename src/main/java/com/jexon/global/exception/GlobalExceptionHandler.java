package com.jexon.global.exception;

import com.jexon.comment.exception.CommentNotFoundException;
import com.jexon.comment.exception.CommentPermissionDeniedException;
import com.jexon.global.exception.dto.ErrorResponse;
import com.jexon.post.exception.PostNotFoundException;
import com.jexon.post.exception.PostPermissionDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    // 로그인 실패
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException exception){
        ErrorResponse response = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                "아이디 또는 비밀번호가 올바르지 않습니다."
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    // 게시글을 찾을 수 없음
    @ExceptionHandler(PostNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePostNotFound(PostNotFoundException exception){
        ErrorResponse response = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                exception.getMessage()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // 게시글 권한 부족
    @ExceptionHandler(PostPermissionDeniedException.class)
    public ResponseEntity<ErrorResponse> handlePostPermissionDenied(PostPermissionDeniedException exception){
        ErrorResponse response = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                exception.getMessage()
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    // 댓글을 찾을 수 없음
    @ExceptionHandler(CommentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCommentNotFound(CommentNotFoundException exception){
        ErrorResponse response = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                exception.getMessage()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // 댓글 권한 부족
    @ExceptionHandler(CommentPermissionDeniedException.class)
    public ResponseEntity<ErrorResponse> handleCommentPermissionDenied(CommentPermissionDeniedException exception){
        ErrorResponse response = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                exception.getMessage()
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }
}
