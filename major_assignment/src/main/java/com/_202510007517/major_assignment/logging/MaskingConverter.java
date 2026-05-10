package com._202510007517.major_assignment.logging;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 敏感字段打码转换器（R4.2 / Property 6 敏感字段打码不变量 / Design §7.3）。
 *
 * <p>匹配常见密钥/令牌字段名，将其值统一替换为 {@value #MASK}，支持以下常见形态：</p>
 * <ul>
 *   <li>JSON 键值：{@code "password":"secret"} → {@code "password":"***"}</li>
 *   <li>Java Bean toString：{@code password=secret} / {@code password=secret,} → {@code password=***}</li>
 *   <li>URL / query 参数：{@code token=abc&foo=1} → {@code token=***&foo=1}</li>
 *   <li>YAML/properties 风格：{@code api-key: xxx} / {@code secret : xxx}</li>
 * </ul>
 *
 * <p>敏感键名（大小写不敏感）：{@code password | pwd | apiKey | api_key | api-key | secret | token | credential}。</p>
 *
 * <p>在 {@code logback-spring.xml} 中通过 {@code <conversionRule>} 注册为 {@code %mask}，
 * 在格式串中用 {@code %mask(%msg)} 包裹待打码字段即可（Controller pattern）。</p>
 *
 * <p>注意：本转换器仅作用于 Logback pattern 输出流；JSON 输出路径由
 * {@link MessageMaskingJsonProvider} 负责，两者共享相同的正则实现（{@link #mask(String)}）。</p>
 */
public class MaskingConverter extends ClassicConverter {

    /** 统一掩码文本。 */
    public static final String MASK = "***";

    /**
     * 复合正则，分三个分支匹配：
     * <pre>
     *   1) "key":"value"    —— JSON 字符串值
     *   2) "key":NUMBER     —— JSON 非字符串值（数字 / true / false / null）
     *   3) key=value | key: value —— toString / query / yaml / properties
     * </pre>
     *
     * <p>使用 named groups 便于替换时引用原始键与分隔符。</p>
     */
    static final Pattern SENSITIVE_PATTERN = Pattern.compile(
            "(?i)"
                    // 分支 1：JSON 字符串 "key":"value"
                    + "(?<jsonKey>\"(?:password|pwd|api[_-]?key|secret|token|credential)\"\\s*:\\s*)"
                    + "\"(?:[^\"\\\\]|\\\\.)*\""
                    + "|"
                    // 分支 2：JSON 非字符串值 "key":123 / "key":true
                    + "(?<jsonNumKey>\"(?:password|pwd|api[_-]?key|secret|token|credential)\"\\s*:\\s*)"
                    + "(?:true|false|null|-?\\d+(?:\\.\\d+)?)"
                    + "|"
                    // 分支 3：plain key=value / key: value（值以逗号/分号/空白/} /] /换行 / & 为界）
                    + "(?<plainKey>\\b(?:password|pwd|api[_-]?key|secret|token|credential)\\b\\s*[:=]\\s*)"
                    + "(?<plainVal>(?:\"[^\"]*\"|'[^']*'|[^,;\\s}\\]&]+))"
    );

    @Override
    public String convert(ILoggingEvent event) {
        if (event == null) {
            return "";
        }
        String formatted = event.getFormattedMessage();
        return mask(formatted);
    }

    /**
     * 对任意字符串执行敏感字段掩码替换。线程安全（{@link Pattern} 只读、{@link Matcher} 为局部变量）。
     *
     * @param input 原始日志文本；{@code null} 或空串原样返回。
     * @return 掩码后的文本；不含任何敏感字段时原样返回。
     */
    public static String mask(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        Matcher matcher = SENSITIVE_PATTERN.matcher(input);
        if (!matcher.find()) {
            return input;
        }
        StringBuilder sb = new StringBuilder(input.length());
        int cursor = 0;
        do {
            sb.append(input, cursor, matcher.start());
            String jsonKey = matcher.group("jsonKey");
            String jsonNumKey = matcher.group("jsonNumKey");
            String plainKey = matcher.group("plainKey");
            if (jsonKey != null) {
                sb.append(jsonKey).append('"').append(MASK).append('"');
            } else if (jsonNumKey != null) {
                sb.append(jsonNumKey).append('"').append(MASK).append('"');
            } else if (plainKey != null) {
                sb.append(plainKey).append(MASK);
            }
            cursor = matcher.end();
        } while (matcher.find());
        sb.append(input, cursor, input.length());
        return sb.toString();
    }
}
