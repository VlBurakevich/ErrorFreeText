package com.solution.errorfreetext.dto;

import java.util.List;

public record SpellerError(
        String word,
        int pos,
        int len,
        int code,
        List<String> s
) {
}
