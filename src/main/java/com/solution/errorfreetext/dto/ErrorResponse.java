package com.solution.errorfreetext.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

public record ErrorResponse(
        String errorMessage,
        int errorCode,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
        String timestamp,
        String path
) {
}
