package com.solution.errorfreetext.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    public YandexSpellerService(@Value("{app.yandex-speller-url}") String yandexSpellUrl) {
        this.restClient = RestClient.create(yandexSpellUrl);
    }

    public String correctText(String text, String lang) {

    }
}
