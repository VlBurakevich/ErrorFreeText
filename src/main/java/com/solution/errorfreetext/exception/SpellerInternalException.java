package com.solution.errorfreetext.exception;

public class SpellerInternalException extends AppException {
    public SpellerInternalException(Throwable cause) {
        super(ErrorCode.YANDEX_SPELLER_INTERNAL_ERROR, cause);
    }
}
