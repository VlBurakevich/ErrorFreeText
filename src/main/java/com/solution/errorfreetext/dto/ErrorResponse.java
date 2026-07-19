package com.solution.errorfreetext.dto;

public record ErrorResponse(
        String errorMessage,
        int errorCode,
        String timestamp,
        String path
) {
}
