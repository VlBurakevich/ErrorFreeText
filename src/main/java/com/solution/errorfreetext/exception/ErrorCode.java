package com.solution.errorfreetext.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    TASK_NOT_FOUND(40401, HttpStatus.NOT_FOUND, "Task with id: %s not found"),

    INVALID_ARGUMENTS(40001, HttpStatus.BAD_REQUEST, "Validation failed: %s"),
    INVALID_LANGUAGE(40002, HttpStatus.BAD_REQUEST, "Unsupported language: %s. Supported languages are: RU, EN"),
    TEXT_TOO_LONG(40003, HttpStatus.BAD_REQUEST, "Text size exceeds maximum allowed limit"),
    INVALID_PARAMETER_FORMAT(40004, HttpStatus.BAD_REQUEST, "Invalid request parameter format: %s"),

    TASK_ALREADY_PROCESSING(42201, HttpStatus.UNPROCESSABLE_ENTITY, "Task %s is already being processed"),

    YANDEX_SPELLER_UNAVAILABLE(50201, HttpStatus.BAD_GATEWAY, "Yandex Speller API is unavailable or returned an error"),
    YANDEX_SPELLER_TIMEOUT(50202, HttpStatus.BAD_GATEWAY, "Timeout waiting for Yandex Speller response"),

    INTERNAL_SERVER_ERROR(50000, HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred"),
    YANDEX_SPELLER_INTERNAL_ERROR(50001, HttpStatus.INTERNAL_SERVER_ERROR, "Internal error during text correction"),

    METHOD_NOT_ALLOWED(40500, HttpStatus.METHOD_NOT_ALLOWED, "HTTP method is not supported for this endpoint"),
    UNSUPPORTED_MEDIA_TYPE(41500, HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Content-Type is not supported");

    private final int code;
    private final HttpStatus httpStatus;
    private final String messageTemplate;

    ErrorCode(int code, HttpStatus httpStatus, String messageTemplate) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.messageTemplate = messageTemplate;
    }
}

