package com.solution.errorfreetext.entity;

import com.solution.errorfreetext.exception.InvalidLanguageException;

public enum TextLanguage {
    RU, EN;

    public static TextLanguage parse(String lang) {
        if (lang == null || lang.isBlank()) {
            throw new InvalidLanguageException("null/empty");
        }
        try {
            return TextLanguage.valueOf(lang.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            throw new InvalidLanguageException(lang);
        }
    }
}
