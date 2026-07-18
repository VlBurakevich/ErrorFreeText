package com.solution.errorfreetext.exception;

import com.solution.errorfreetext.entity.TextLanguage;

public class InvalidLanguageException extends AppException {
    public InvalidLanguageException(String lang) {
        super(ErrorCode.INVALID_LANGUAGE, lang);
    }

    public InvalidLanguageException(TextLanguage lang) {
        super(ErrorCode.INVALID_LANGUAGE, lang.toString());
    }
}
