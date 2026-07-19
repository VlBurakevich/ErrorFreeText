package com.solution.errorfreetext.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import com.solution.errorfreetext.dto.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(AppException ex, HttpServletRequest request) {
        ErrorCode err = ex.getErrorCode();
        String path = request.getRequestURI();

        if (err.getHttpStatus().is5xxServerError()) {
            log.error("Server-side app error occurred [{}]: {} on path: {}", err.getCode(), ex.getMessage(), path, ex);
        } else {
            log.warn("Client-side app error occurred [{}]: {} on path: {}", err.getCode(), ex.getMessage(), path);
        }

        return buildResponse(err, ex.getMessage(), path);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String path = request.getRequestURI();
        String validationErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        log.warn("Validation failed on path {}: {}", path, validationErrors);

        return buildResponse(ErrorCode.INVALID_ARGUMENTS, validationErrors, path);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        String path = request.getRequestURI();
        log.error("Unhandled critical exception on path {}", path, ex);

        return buildResponse(ErrorCode.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_SERVER_ERROR.getMessageTemplate(), path);
    }

    private ResponseEntity<ErrorResponse> buildResponse(ErrorCode err, String message, String path) {
        ErrorResponse response = new ErrorResponse(
                message,
                err.getCode(),
                OffsetDateTime.now(),
                path
        );
        return ResponseEntity.status(err.getHttpStatus()).body(response);
    }
}
