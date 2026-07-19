package com.solution.errorfreetext.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.OffsetDateTime;

public record ErrorResponse(
        String errorMessage,
        int errorCode,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
        OffsetDateTime timestamp,
        String path
) {
}
