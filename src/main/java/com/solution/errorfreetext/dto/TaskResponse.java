package com.solution.errorfreetext.dto;

import com.solution.errorfreetext.entity.TaskStatus;

public record TaskResponse(
        TaskStatus status,
        String correctedText,
        String errorMessage
) {
}
