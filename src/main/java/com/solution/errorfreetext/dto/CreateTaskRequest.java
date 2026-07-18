package com.solution.errorfreetext.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateTaskRequest(
        @NotBlank(message = "Text cannot be empty")
        @Size(min = 3, message = "Text must be at least 3 characters long")
        @Pattern(regexp = ".*[a-zA-Zа-яА-ЯёЁ].*", message = "Text cannot consist only of digits and special characters")
        String text,

        @NotBlank(message = "Language cannot be empty")
        @Pattern(regexp = "^(EN|RU)$", message = "Language must be either EN or RU")
        String language
) {
}
