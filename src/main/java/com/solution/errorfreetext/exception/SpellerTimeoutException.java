package com.solution.errorfreetext.exception;

public class SpellerTimeoutException extends AppException {
    public SpellerTimeoutException(Throwable cause) {
        super(ErrorCode.YANDEX_SPELLER_TIMEOUT, cause);
    }
}
