package com._202510007517.platform.agent.internet;

import com._202510007517.platform.agent.config.AgentInternetProperties;

import java.net.URI;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebPageReadService {
    private static final Pattern TITLE_PATTERN = Pattern.compile("(?is)<title[^>]*>(.*?)</title>");

    private final AgentInternetProperties properties;
    private final InternetAccessPolicy accessPolicy;
    private final InternetHttpClient httpClient;

    public WebPageReadService(AgentInternetProperties properties,
                              InternetAccessPolicy accessPolicy,
                              InternetHttpClient httpClient) {
        this.properties = properties;
        this.accessPolicy = accessPolicy;
        this.httpClient = httpClient;
    }

    public WebPageReadResponse read(String url) {
        if (!properties.isEnabled()) {
            return new WebPageReadResponse(InternetResultStatus.DISABLED, url, "", "", "联网功能当前未启用。");
        }
        if (url == null || url.isBlank()) {
            return new WebPageReadResponse(InternetResultStatus.VALIDATION_FAILED, url, "", "", "请提供需要读取的网页地址。");
        }
        try {
            URI uri = URI.create(url.trim());
            accessPolicy.validate(uri);
            String html = httpClient.get(uri, Duration.ofSeconds(properties.getTimeoutSeconds()),
                    properties.getMaxPageBytes(), properties.getUserAgent());
            String title = extractTitle(html);
            String content = InternetHtml.truncate(InternetHtml.text(html), properties.getMaxContentChars());
            return new WebPageReadResponse(InternetResultStatus.EXECUTED, uri.toString(), title, content, "网页读取完成。");
        } catch (InternetAccessDeniedException ex) {
            return new WebPageReadResponse(InternetResultStatus.FAILED, url.trim(), "", "", ex.getMessage());
        } catch (RuntimeException ex) {
            return new WebPageReadResponse(InternetResultStatus.FAILED, url.trim(), "", "", "网页读取失败：" + ex.getClass().getSimpleName());
        }
    }

    private String extractTitle(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        Matcher matcher = TITLE_PATTERN.matcher(html);
        if (!matcher.find()) {
            return "";
        }
        return InternetHtml.text(matcher.group(1));
    }
}
