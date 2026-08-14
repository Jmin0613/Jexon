package com.jexon.global.exception;

import com.jexon.comment.exception.CommentNotFoundException;
import com.jexon.comment.exception.CommentPermissionDeniedException;
import com.jexon.gamefile.exception.DuplicateGameFileException;
import com.jexon.gamefile.exception.FileStorageException;
import com.jexon.gamefile.exception.InvalidGameFileException;
import com.jexon.gamefile.exception.GameFileStateException;
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
import org.springframework.web.multipart.MaxUploadSizeExceededException;
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

    // 업로드 요청 파일 검증 실패
    @ExceptionHandler(InvalidGameFileException.class)
    public ResponseEntity<ErrorResponse> handleInvalidGameFile(InvalidGameFileException exception) {
        ErrorResponse response = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                exception.getMessage()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 중복 파일 업드로로 인한 충돌
    @ExceptionHandler(DuplicateGameFileException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateGameFile(DuplicateGameFileException exception) {
        ErrorResponse response = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                exception.getMessage()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    // 실제 저장소 I/O 등 서버 내부 저장 실패
    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<ErrorResponse> handleFileStorage(FileStorageException exception) {
        ErrorResponse response = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "파일 저장 중 오류가 발생했습니다."
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(GameFileStateException.class)
    public ResponseEntity<ErrorResponse> handleGameFileState(GameFileStateException exception) {
        ErrorResponse response = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                exception.getMessage()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    // Multipart 단계에서 최대 업로드 크기 초과
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException exception) {
        ErrorResponse response = new ErrorResponse(
                HttpStatus.PAYLOAD_TOO_LARGE.value(),
                "업로드 가능한 최대 파일 크기를 초과했습니다."
        );

        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(response);
    }
}
