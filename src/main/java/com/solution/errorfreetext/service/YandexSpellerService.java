package com.solution.errorfreetext.service;

import com.solution.errorfreetext.dto.SpellerError;
import com.solution.errorfreetext.entity.TextLanguage;
import com.solution.errorfreetext.exception.InvalidLanguageException;
import com.solution.errorfreetext.exception.SpellerIntegrationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.regex.Pattern;

@Slf4j
@Service
public class YandexSpellerService {
    private final RestClient restClient;

    private static final Pattern DIGITAL_PATTERN = Pattern.compile(".*\\d.*");
    private static final Pattern URL_PATTERN = Pattern.compile(".*https?://.*|.*www\\..*");

    private static final int IGNORE_DIGITS = 2;
    private static final int IGNORE_URLS = 4;

    public YandexSpellerService(
            @Value("${app.yandex-speller.base-url}") String baseUrl,
            @Value("${app.yandex-speller.connect-timeout}") int connectTimeout,
            @Value("${app.yandex-speller.read-timeout}") int readTimeout
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader("User-Agent", "ErrorFreeText-Backend/1.0")
                .build();
    }

    public String correctText(String text, TextLanguage lang) {
        if (text == null || text.isBlank()) {
            return text;
        }

        if (lang == null) {
            throw new InvalidLanguageException("Language cannot be null");
        }

        int options = 0;
        if (DIGITAL_PATTERN.matcher(text).matches()) {
            options += IGNORE_DIGITS;
        }
        if (URL_PATTERN.matcher(text).matches()) {
            options += IGNORE_URLS;
        }

        try {
            SpellerError[] errors = sendSpellerRequest(text, lang, options);

            if (errors == null || errors.length == 0) {
                return text;
            }

            return applyCorrections(text, errors);
        } catch (Exception e) {
            log.error("Yandex Speller request failed for text: {}", text, e);
            throw new SpellerIntegrationException(e);
        }
    }

    private SpellerError[] sendSpellerRequest(String text, TextLanguage lang, int options) {
        SpellerError[][] errorsResult = restClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/checkTexts")
                        .queryParam("lang", lang.name())
                        .queryParam("options", options)
                        .build())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body("text=" + text)
                .retrieve()
                .body(SpellerError[][].class);

        if (errorsResult == null || errorsResult.length == 0 || errorsResult[0].length == 0) {
            return new SpellerError[0];
        }

        return errorsResult[0];
    }

    private static String applyCorrections(String text, SpellerError[] errors) {
        StringBuilder correctedText = new StringBuilder(text);

        for (int i = errors.length - 1; i >= 0; i--) {
            SpellerError error = errors[i];

            if (error.s() != null && !error.s().isEmpty()) {
                String firstSuggestion = error.s().getFirst();
                int start = error.pos();
                int end = start + error.len();

                if (start >= 0 && end <= correctedText.length()) {
                    correctedText.replace(start, end, firstSuggestion);
                }
            }
        }
        return correctedText.toString();
    }
}
