package com._202510007517.platform.agent.internet;

import org.springframework.web.util.HtmlUtils;

import java.util.regex.Pattern;

final class InternetHtml {
    private static final Pattern HEAD_BLOCK = Pattern.compile("(?is)<head\\b[^>]*>.*?</head>");
    private static final Pattern SCRIPT_BLOCKS = Pattern.compile("(?is)<(script|style|noscript|svg)\\b[^>]*>.*?</\\1>");
    private static final Pattern TAGS = Pattern.compile("(?is)<[^>]+>");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private InternetHtml() {
    }

    static String text(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        String withoutHead = HEAD_BLOCK.matcher(html).replaceAll(" ");
        String withoutScript = SCRIPT_BLOCKS.matcher(withoutHead).replaceAll(" ");
        String withoutTags = TAGS.matcher(withoutScript).replaceAll(" ");
        return normalize(HtmlUtils.htmlUnescape(withoutTags));
    }

    static String normalize(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return WHITESPACE.matcher(text).replaceAll(" ").trim();
    }

    static String truncate(String text, int maxChars) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxChars) {
            return text;
        }
        return text.substring(0, Math.max(0, maxChars)).trim();
    }
}
