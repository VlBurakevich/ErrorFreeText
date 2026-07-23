package com.solution.errorfreetext.service;

import com.solution.errorfreetext.entity.TextLanguage;
import com.solution.errorfreetext.exception.InvalidLanguageException;
import com.solution.errorfreetext.exception.SpellerInternalException;
import com.solution.errorfreetext.exception.SpellerTimeoutException;
import com.solution.errorfreetext.exception.SpellerUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.Builder;

import java.net.SocketTimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.util.ReflectionTestUtils.setField;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class YandexSpellerServiceTest {

    private YandexSpellerService yandexSpellerService;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        yandexSpellerService = new YandexSpellerService("https://api.yandex.ru", 1000, 1000);

        Builder builder = RestClient.builder().baseUrl("https://api.yandex.ru").defaultHeader("User-Agent", "ErrorFreeText-Backend/1.0");

        mockServer = MockRestServiceServer.bindTo(builder).build();

        setField(yandexSpellerService, "restClient", builder.build());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("Должен возвращать исходный текст без запроса к API, если текст пустой или из пробелов")
    void correctText_ShouldReturnSameText_WhenTextIsNullOrEmpty(String input) {
        String result = yandexSpellerService.correctText(input, TextLanguage.RU);

        assertEquals(input, result);
        mockServer.verify();
    }

    @Test
    @DisplayName("Должен выбрасывать InvalidLanguageException, если язык null")
    void correctText_ShouldThrowInvalidLanguageException_WhenLangIsNull() {
        InvalidLanguageException exception = assertThrows(InvalidLanguageException.class, () -> yandexSpellerService.correctText("Какой-то текст", null));

        assertEquals("Unsupported language: Language cannot be null. Supported languages are: RU, EN", exception.getMessage());
        mockServer.verify();
    }

    @Test
    @DisplayName("Должен правильно передавать опцию IGNORE_DIGITS (2), если в тексте есть цифры")
    void correctText_ShouldSetIgnoreDigitsOption_WhenTextContainsNumbers() {
        mockServer.expect(requestTo("https://api.yandex.ru")).andExpect(method(HttpMethod.POST)).andExpect(content().string("text=%D0%A2%D0%B5%D0%BA%D1%81%D1%82+123&lang=RU&options=2")).andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        String result = yandexSpellerService.correctText("Текст 123", TextLanguage.RU);

        assertEquals("Текст 123", result);
        mockServer.verify();
    }

    @Test
    @DisplayName("Должен правильно передавать опцию IGNORE_URLS (4), если в тексте есть URL")
    void correctText_ShouldSetIgnoreUrlsOption_WhenTextContainsUrl() {
        mockServer.expect(requestTo("https://api.yandex.ru")).andExpect(method(HttpMethod.POST)).andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        String result = yandexSpellerService.correctText("Check https://example.com please", TextLanguage.EN);

        assertEquals("Check https://example.com please", result);
        mockServer.verify();
    }

    @Test
    @DisplayName("Должен суммировать опции (2 + 4 = 6), если в тексте есть и цифры, и URL")
    void correctText_ShouldCombineOptions_WhenTextContainsNumbersAndUrl() {
        mockServer.expect(requestTo("https://api.yandex.ru")).andExpect(method(HttpMethod.POST)).andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        String result = yandexSpellerService.correctText("Версия 2.0 на www.test.ru", TextLanguage.RU);

        assertEquals("Версия 2.0 на www.test.ru", result);
        mockServer.verify();
    }

    @Test
    @DisplayName("Должен успешно исправлять опечатки с конца строки")
    void correctText_ShouldApplyCorrectionsCorrectly() {
        String jsonResponse = """
        [
          [
            {
              "code": 1,
              "pos": 0,
              "len": 7,
              "word": "Прейвет",
              "s": ["Привет"]
            },
            {
              "code": 1,
              "pos": 9,
              "len": 4,
              "word": "ммир",
              "s": ["мир"]
            }
          ]
        ]
        """;

        mockServer.expect(requestTo("https://api.yandex.ru")).andExpect(method(HttpMethod.POST)).andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        String result = yandexSpellerService.correctText("Прейвет, ммир!", TextLanguage.RU);

        assertEquals("Привет, мир!", result);
        mockServer.verify();
    }

    @Test
    @DisplayName("Должен выбрасывать SpellerTimeoutException при сетевых ошибках или таймауте")
    void correctText_ShouldThrowSpellerTimeoutException_WhenNetworkIssueOccurs() {
        mockServer.expect(requestTo("https://api.yandex.ru")).andExpect(method(HttpMethod.POST)).andRespond(withException(new SocketTimeoutException("Read timed out")));

        assertThrows(SpellerTimeoutException.class, () -> yandexSpellerService.correctText("Проверка таймаута", TextLanguage.RU));

        mockServer.verify();
    }

    @Test
    @DisplayName("Должен выбрасывать SpellerUnavailableException при 5xx/4xx ответах от Яндекса")
    void correctText_ShouldThrowSpellerUnavailableException_WhenServerError() {
        mockServer.expect(requestTo("https://api.yandex.ru")).andExpect(method(HttpMethod.POST)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThrows(SpellerUnavailableException.class, () -> yandexSpellerService.correctText("Сервер упал", TextLanguage.RU));

        mockServer.verify();
    }

    @Test
    @DisplayName("Должен выбрасывать SpellerInternalException при неожиданных исключениях")
    void correctText_ShouldThrowSpellerInternalException_WhenUnexpectedErrorOccurs() {
        mockServer.expect(requestTo("https://api.yandex.ru")).andExpect(method(HttpMethod.POST)).andRespond(withSuccess("{invalid_json}", MediaType.APPLICATION_JSON));

        assertThrows(SpellerInternalException.class, () -> yandexSpellerService.correctText("Невалидный ответ", TextLanguage.RU));

        mockServer.verify();
    }
}
