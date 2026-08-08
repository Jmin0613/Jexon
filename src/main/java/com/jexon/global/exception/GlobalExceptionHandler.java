package com.jexon.global.exception;

import com.jexon.comment.exception.CommentNotFoundException;
import com.jexon.comment.exception.CommentPermissionDeniedException;
import com.jexon.gameversion.exception.DuplicateGameVersionException;
import com.jexon.gameversion.exception.GameVersionConcurrencyConflictException;
import com.jexon.gameversion.exception.GameVersionNotFoundException;
import com.jexon.gameversion.exception.GameVersionPermissionDeniedException;
import com.jexon.gameversion.exception.InvalidGameVersionStateException;
import com.jexon.global.exception.dto.ErrorResponse;
import com.jexon.news.exception.NewsNotFoundException;
import com.jexon.news.exception.NewsPermissionDeniedException;
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

    // 새소식을 찾을 수 없음
    @ExceptionHandler(NewsNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNewsNotFound(NewsNotFoundException exception){
        ErrorResponse response = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                exception.getMessage()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // 새소식 권한 부족
    @ExceptionHandler(NewsPermissionDeniedException.class)
    public ResponseEntity<ErrorResponse> handleNewsPermissionDenied(NewsPermissionDeniedException exception){
        ErrorResponse response = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                exception.getMessage()
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    // 게임 버전을 찾을 수 없음
    @ExceptionHandler(GameVersionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleGameVersionNotFound(GameVersionNotFoundException exception){
        ErrorResponse response = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                exception.getMessage()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // 게임 버전 권한 부족
    @ExceptionHandler(GameVersionPermissionDeniedException.class)
    public ResponseEntity<ErrorResponse> handleGameVersionPermissionDenied(GameVersionPermissionDeniedException exception){
        ErrorResponse response = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                exception.getMessage()
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    // 게임 버전 중복
    @ExceptionHandler(DuplicateGameVersionException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateGameVersion(DuplicateGameVersionException exception){
        ErrorResponse response = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                exception.getMessage()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    // 게임 버전 상태 전이 불가
    @ExceptionHandler(InvalidGameVersionStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidGameVersionState(InvalidGameVersionStateException exception){
        ErrorResponse response = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                exception.getMessage()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    // 게임 버전 동시 변경 충돌
    @ExceptionHandler(GameVersionConcurrencyConflictException.class)
    public ResponseEntity<ErrorResponse> handleGameVersionConcurrencyConflict(GameVersionConcurrencyConflictException exception){
        ErrorResponse response = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                exception.getMessage()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }
}
