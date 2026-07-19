package com.solution.errorfreetext.exception;

public class SpellerUnavailableException extends AppException {
    public SpellerUnavailableException(Throwable cause) {
        super(ErrorCode.YANDEX_SPELLER_UNAVAILABLE, cause);
    }
}
