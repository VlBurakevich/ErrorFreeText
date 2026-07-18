package com.solution.errorfreetext.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateTaskRequest {

    @NotBlank(message = "Text cannot be empty")
    @Size(min = 3, message = "Text must be at least 3 characters long")
    @Pattern(regexp = ".*[a-zA-Zа-яА-ЯёЁ].*", message = "Text cannot consist only of digits and special characters")
    private String text;

    @NotBlank(message = "Language cannot be empty")
    @Pattern(regexp = "^(EN|RU)$", message = "Language must be either EN or RU")
    private String language;
}
