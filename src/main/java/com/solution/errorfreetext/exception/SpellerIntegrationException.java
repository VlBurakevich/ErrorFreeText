package com.solution.errorfreetext.exception;

public class SpellerIntegrationException extends AppException {
    public SpellerIntegrationException(Throwable cause) {
        super(ErrorCode.YANDEX_SPELLER_TIMEOUT, cause);
    }
}
