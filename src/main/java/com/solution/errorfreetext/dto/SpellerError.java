package com.solution.errorfreetext.dto;

import java.util.List;

public record SpellerError(
        String word,
        List<String> s
) {
}
