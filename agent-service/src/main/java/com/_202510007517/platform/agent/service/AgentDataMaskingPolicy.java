package com._202510007517.platform.agent.service;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AgentDataMaskingPolicy {
    private static final Pattern MOBILE = Pattern.compile("(?<!\\d)(1[3-9]\\d)\\d{4}(\\d{4})(?!\\d)");
    private static final Pattern EMAIL = Pattern.compile("([A-Za-z0-9._%+-])([A-Za-z0-9._%+-]*)(@[A-Za-z0-9.-]+\\.[A-Za-z]{2,})");
    private static final Pattern ID_CARD = Pattern.compile("(?<!\\d)(\\d{6})\\d{8}(\\d{3}[0-9Xx])(?!\\d)");
    private static final Pattern CREDENTIAL = Pattern.compile(
            "(?i)(\"?\\b(password|token|apiKey|api_key|secret)\\b\"?\\s*[:=]\\s*)(\"?)[^\\s,;，；}\"']+(\"?)"
    );

    public String maskText(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        String masked = MOBILE.matcher(text).replaceAll("$1****$2");
        masked = EMAIL.matcher(masked).replaceAll("$1***$3");
        masked = ID_CARD.matcher(masked).replaceAll("$1********$2");
        return maskCredentials(masked);
    }

    public Map<String, Object> maskMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> masked = new LinkedHashMap<>();
        metadata.forEach((key, value) -> masked.put(key, maskValueForKey(key, value)));
        return masked;
    }

    private Object maskValueForKey(String key, Object value) {
        if (isSensitiveMetadataKey(key)) {
            return "***";
        }
        return maskValue(value);
    }

    private Object maskValue(Object value) {
        if (value instanceof String stringValue) {
            return maskText(stringValue);
        }
        if (value instanceof Map<?, ?> mapValue) {
            Map<String, Object> masked = new LinkedHashMap<>();
            mapValue.forEach((key, nestedValue) -> {
                String keyText = String.valueOf(key);
                masked.put(keyText, maskValueForKey(keyText, nestedValue));
            });
            return masked;
        }
        if (value instanceof Iterable<?> iterable) {
            return streamFrom(iterable).map(this::maskValue).toList();
        }
        return value;
    }

    private java.util.stream.Stream<?> streamFrom(Iterable<?> iterable) {
        if (iterable instanceof List<?> list) {
            return list.stream();
        }
        return java.util.stream.StreamSupport.stream(iterable.spliterator(), false);
    }

    private String maskCredentials(String text) {
        Matcher matcher = CREDENTIAL.matcher(text);
        StringBuffer masked = new StringBuffer();
        while (matcher.find()) {
            String replacement = matcher.group(1) + matcher.group(3) + "***" + matcher.group(4);
            matcher.appendReplacement(masked, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(masked);
        return masked.toString();
    }

    private boolean isSensitiveMetadataKey(String key) {
        String normalized = key.replaceAll("[_\\-\\s]", "").toLowerCase(Locale.ROOT);
        return normalized.equals("password")
                || normalized.equals("token")
                || normalized.equals("apikey")
                || normalized.equals("secret");
    }
}
