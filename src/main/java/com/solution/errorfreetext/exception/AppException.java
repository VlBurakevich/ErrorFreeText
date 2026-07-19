package com.solution.errorfreetext.exception;

import lombok.Getter;

@Getter
public abstract class AppException extends RuntimeException {
    private final ErrorCode errorCode;

    protected AppException(ErrorCode errorCode, Object... args) {
        super(String.format(errorCode.getMessageTemplate(), args));
        this.errorCode = errorCode;
    }

    protected AppException(ErrorCode errorCode, Throwable cause, Object... args) {
        super(String.format(errorCode.getMessageTemplate(), args), cause);
        this.errorCode = errorCode;
    }
}
