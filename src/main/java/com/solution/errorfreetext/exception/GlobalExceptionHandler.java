package com.solution.errorfreetext.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import com.solution.errorfreetext.dto.ErrorResponse;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

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

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ErrorResponse> handleBadRequestExceptions(Exception ex, HttpServletRequest request) {
        String msg = (ex instanceof MethodArgumentTypeMismatchException mismatch)
                ? String.format("Invalid value '%s' for parameter '%s'", mismatch.getValue(), mismatch.getName())
                : "Request body is malformed or invalid";

        log.warn("Bad request exception on path {}: {}", request.getRequestURI(), msg);
        return buildResponse(ErrorCode.INVALID_ARGUMENTS, msg, request.getRequestURI());
    }

    @ExceptionHandler({HttpRequestMethodNotSupportedException.class, HttpMediaTypeNotSupportedException.class})
    public ResponseEntity<ErrorResponse> handleHttpProtocolExceptions(Exception ex, HttpServletRequest request) {
        ErrorCode err;
        String msg;

        if (ex instanceof HttpRequestMethodNotSupportedException methodEx) {
            err = ErrorCode.METHOD_NOT_ALLOWED;
            msg = String.format("HTTP method '%s' is not supported for this endpoint", methodEx.getMethod());
        } else {
            err = ErrorCode.UNSUPPORTED_MEDIA_TYPE;
            msg = String.format("Content-Type '%s' is not supported. Expected 'application/json'", ((HttpMediaTypeNotSupportedException) ex).getContentType());
        }

        log.warn("HTTP protocol exception on path {}: {}", request.getRequestURI(), msg);
        return buildResponse(err, msg, request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        String path = request.getRequestURI();
        log.error("Unhandled critical exception on path {}", path, ex);

        return buildResponse(ErrorCode.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_SERVER_ERROR.getMessageTemplate(), path);
    }

    private ResponseEntity<ErrorResponse> buildResponse(ErrorCode err, String message, String path) {
        String formattedTimestamp = LocalDateTime.now().format(ISO_FORMATTER);

        ErrorResponse response = new ErrorResponse(
                message,
                err.getCode(),
                formattedTimestamp,
                path
        );
        return ResponseEntity.status(err.getHttpStatus()).body(response);
    }
}
