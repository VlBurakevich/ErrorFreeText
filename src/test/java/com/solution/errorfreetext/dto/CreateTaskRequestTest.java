package com.solution.errorfreetext.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateTaskRequestTest {
    private Validator validator;

    @BeforeEach
    void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    @DisplayName("Валидация проходит успешно при корректных данных")
    void shouldPassValidation_WhenRequestIsValid() {
        CreateTaskRequest request = new CreateTaskRequest("Привет мир", "RU");
        Set<ConstraintViolation<CreateTaskRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"FR", "ru", "en", "DE", "123"})
    @DisplayName("Валидация языка падает, если язык не EN и не RU")
    void shouldFailValidation_WhenLanguageIsInvalid(String invalidLanguage) {
        CreateTaskRequest request = new CreateTaskRequest("Valid text", invalidLanguage);
        Set<ConstraintViolation<CreateTaskRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
        assertEquals("Language must be either EN or RU", violations.iterator().next().getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345", "!!!???", "---++--"})
    @DisplayName("Валидация текста падает, если текст состоит только из цифр и спецсимволов")
    void shouldFailValidation_WhenTextHasNoLetters(String invalidText) {
        CreateTaskRequest request = new CreateTaskRequest(invalidText, "EN");
        Set<ConstraintViolation<CreateTaskRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
        assertEquals("Text cannot consist only of digits and special characters", violations.iterator().next().getMessage());
    }
}
