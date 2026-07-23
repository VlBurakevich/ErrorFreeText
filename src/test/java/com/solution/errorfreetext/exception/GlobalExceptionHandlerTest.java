package com.solution.errorfreetext.exception;

import com.solution.errorfreetext.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    private static final String TEST_PATH = "/api/v1/tasks";

    @BeforeEach
    void setUp() {
        when(request.getRequestURI()).thenReturn(TEST_PATH);
    }

    @Nested
    @DisplayName("Тесты обработки кастомных ошибок (AppException)")
    class AppExceptionTests {

        @Test
        @DisplayName("handleAppException: должен корректно обрабатывать клиентские ошибки (4xx)")
        void handleAppException_ShouldReturnClientErrorResponse() {
            UUID taskId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

            TaskNotFoundException exception = new TaskNotFoundException(taskId);
            ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleAppException(exception, request);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("Task with id: 123e4567-e89b-12d3-a456-426614174000 not found", response.getBody().errorMessage());
            assertEquals(40401, response.getBody().errorCode());
            assertEquals(TEST_PATH, response.getBody().path());
            assertNotNull(response.getBody().timestamp());
        }

        @Test
        @DisplayName("handleAppException: должен корректно обрабатывать серверные ошибки (5xx)")
        void handleAppException_ShouldReturnServerErrorResponse() {
            SpellerUnavailableException exception = new SpellerUnavailableException(new RuntimeException("test"));
            ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleAppException(exception, request);

            assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("Yandex Speller API is unavailable or returned an error", response.getBody().errorMessage());
            assertEquals(50201, response.getBody().errorCode());
            assertEquals(TEST_PATH, response.getBody().path());
        }
    }

    @Nested
    @DisplayName("Тесты ошибок валидации и плохого формата запросов (400 Bad Request)")
    class BadRequestTests {

        @Test
        @DisplayName("handleValidationException: должен форматировать ошибки валидации полей в одну строку")
        void handleValidationException_ShouldFormatMultipleFieldErrors() {
            BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "target");
            bindingResult.addError(new FieldError("target", "text", "Text cannot be empty"));
            bindingResult.addError(new FieldError("target", "language", "Language must be either EN or RU"));

            MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
            when(ex.getBindingResult()).thenReturn(bindingResult);
            ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleValidationException(ex, request);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(ErrorCode.INVALID_ARGUMENTS.getCode(), response.getBody().errorCode());
            assertEquals("text: Text cannot be empty; language: Language must be either EN or RU", response.getBody().errorMessage());
            assertEquals(TEST_PATH, response.getBody().path());
        }

        @Test
        @DisplayName("handleBadRequestExceptions: должен обрабатывать расхождение типов аргументов (MethodArgumentTypeMismatchException)")
        void handleBadRequestExceptions_ShouldHandleTypeMismatch() {
            MethodParameter methodParameter = mock(MethodParameter.class);

            MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                    "abc", UUID.class, "taskId", methodParameter, new IllegalArgumentException("Type mismatch")
            );

            ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleBadRequestExceptions(ex, request);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(ErrorCode.INVALID_ARGUMENTS.getCode(), response.getBody().errorCode());
            assertEquals("Invalid value 'abc' for parameter 'taskId'", response.getBody().errorMessage());
        }

        @Test
        @DisplayName("handleBadRequestExceptions: должен обрабатывать невалидный JSON (HttpMessageNotReadableException)")
        void handleBadRequestExceptions_ShouldHandleMalformedJson() {
            HttpInputMessage inputMessage = mock(HttpInputMessage.class);

            HttpMessageNotReadableException ex = new HttpMessageNotReadableException("Malformed JSON", inputMessage);

            ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleBadRequestExceptions(ex, request);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(ErrorCode.INVALID_ARGUMENTS.getCode(), response.getBody().errorCode());
            assertEquals("Request body is malformed or invalid", response.getBody().errorMessage());
        }
    }

    @Nested
    @DisplayName("Тесты ошибок протокола HTTP (405 Method Not Allowed / 415 Unsupported Media Type)")
    class HttpProtocolTests {

        @Test
        @DisplayName("handleHttpProtocolExceptions: должен обрабатывать неверный HTTP-метод")
        void handleHttpProtocolExceptions_ShouldHandleMethodNotSupported() {
            HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("DELETE");

            ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleHttpProtocolExceptions(ex, request);

            assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(ErrorCode.METHOD_NOT_ALLOWED.getCode(), response.getBody().errorCode());
            assertEquals("HTTP method 'DELETE' is not supported for this endpoint", response.getBody().errorMessage());
        }

        @Test
        @DisplayName("handleHttpProtocolExceptions: должен обрабатывать неподдерживаемый Content-Type")
        void handleHttpProtocolExceptions_ShouldHandleUnsupportedMediaType() {
            HttpMediaTypeNotSupportedException ex = new HttpMediaTypeNotSupportedException(
                    MediaType.APPLICATION_XML,
                    List.of(MediaType.APPLICATION_JSON)
            );
            ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleHttpProtocolExceptions(ex, request);

            assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(ErrorCode.UNSUPPORTED_MEDIA_TYPE.getCode(), response.getBody().errorCode());
            assertEquals("Content-Type 'application/xml' is not supported. Expected 'application/json'", response.getBody().errorMessage());
        }
    }

    @Nested
    @DisplayName("Тесты необработанных критических ошибок (500 Internal Server Error)")
    class GenericExceptionTests {

        @Test
        @DisplayName("handleGenericException: должен маскировать критические исключения под дефолтное сообщение 500")
        void handleGenericException_ShouldReturnGeneric500Response() {
            NullPointerException ex = new NullPointerException("Null reference at line 42");

            ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleGenericException(ex, request);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(ErrorCode.INTERNAL_SERVER_ERROR.getCode(), response.getBody().errorCode());
            assertEquals(ErrorCode.INTERNAL_SERVER_ERROR.getMessageTemplate(), response.getBody().errorMessage());
            assertEquals(TEST_PATH, response.getBody().path());
        }
    }
}
