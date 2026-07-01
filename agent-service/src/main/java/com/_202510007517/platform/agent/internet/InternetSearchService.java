package com._202510007517.platform.agent.internet;

import com._202510007517.platform.agent.config.AgentInternetProperties;
import org.springframework.web.util.HtmlUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InternetSearchService {
    private static final Pattern ANCHOR_PATTERN = Pattern.compile("(?is)<a\\b([^>]*)>(.*?)</a>");
    private static final Pattern HREF_PATTERN = Pattern.compile("(?is)href\\s*=\\s*[\"']([^\"']+)[\"']");
    private static final Pattern SNIPPET_PATTERN = Pattern.compile("(?is)<a\\b[^>]*class\\s*=\\s*[\"'][^\"']*result__snippet[^\"']*[\"'][^>]*>(.*?)</a>");
    private static final Pattern BING_RESULT_PATTERN = Pattern.compile("(?is)<li\\b[^>]*class\\s*=\\s*[\"'][^\"']*b_algo[^\"']*[\"'][^>]*>(.*?)</li>");
    private static final Pattern BING_TITLE_LINK_PATTERN = Pattern.compile("(?is)<h2\\b[^>]*>\\s*<a\\b([^>]*)>(.*?)</a>\\s*</h2>");
    private static final Pattern PARAGRAPH_PATTERN = Pattern.compile("(?is)<p\\b[^>]*>(.*?)</p>");
    private static final Set<String> QUERY_STOP_WORDS = Set.of(
            "official", "documentation", "reference", "docs", "doc", "guide", "search", "latest"
    );

    private final AgentInternetProperties properties;
    private final InternetAccessPolicy accessPolicy;
    private final InternetHttpClient httpClient;

    public InternetSearchService(AgentInternetProperties properties,
                                 InternetAccessPolicy accessPolicy,
                                 InternetHttpClient httpClient) {
        this.properties = properties;
        this.accessPolicy = accessPolicy;
        this.httpClient = httpClient;
    }

    public InternetSearchResponse search(String query) {
        if (!properties.isEnabled()) {
            return new InternetSearchResponse(InternetResultStatus.DISABLED, query, List.of(), "联网功能当前未启用。");
        }
        if (query == null || query.isBlank()) {
            return new InternetSearchResponse(InternetResultStatus.VALIDATION_FAILED, query, List.of(), "请提供需要联网搜索的关键词。");
        }
        try {
            String trimmedQuery = query.trim();
            int maxResults = properties.getMaxResults();
            int parseLimit = wantsOfficialSources(trimmedQuery) ? Math.max(maxResults * 3, maxResults) : maxResults;
            List<InternetSearchResult> parsedResults = new ArrayList<>();
            for (String searchQuery : buildSearchQueries(trimmedQuery)) {
                URI uri = buildSearchUri(searchQuery);
                accessPolicy.validate(uri);
                String html = httpClient.get(uri, Duration.ofSeconds(properties.getTimeoutSeconds()),
                        properties.getMaxPageBytes(), properties.getUserAgent());
                mergeUnique(parsedResults, parseResults(html, parseLimit));
                if (parsedResults.size() >= parseLimit) {
                    break;
                }
            }
            List<InternetSearchResult> results = rankResults(trimmedQuery, parsedResults).stream()
                    .limit(maxResults)
                    .toList();
            String message = results.isEmpty() ? "联网搜索完成，但没有解析到结果。" : "联网搜索完成。";
            return new InternetSearchResponse(InternetResultStatus.EXECUTED, trimmedQuery, results, message);
        } catch (InternetAccessDeniedException ex) {
            return new InternetSearchResponse(InternetResultStatus.FAILED, query.trim(), List.of(), ex.getMessage());
        } catch (RuntimeException ex) {
            return new InternetSearchResponse(InternetResultStatus.FAILED, query.trim(), List.of(), "联网搜索失败：" + ex.getClass().getSimpleName());
        }
    }

    private URI buildSearchUri(String query) {
        String baseUrl = properties.getSearchBaseUrl();
        String separator = baseUrl.contains("?") ? "&" : "?";
        return URI.create(baseUrl + separator + "q=" + URLEncoder.encode(query, StandardCharsets.UTF_8));
    }

    private List<String> buildSearchQueries(String query) {
        List<String> queries = new ArrayList<>();
        if (wantsOfficialSources(query)) {
            String topic = cleanOfficialSearchTopic(query);
            String officialDomain = officialDomainFor(topic);
            if (officialDomain != null) {
                queries.add("site:" + officialDomain + " " + topic);
            }
            if (!topic.equalsIgnoreCase(query)) {
                queries.add(topic + " official documentation");
            }
        }
        queries.add(query);
        return dedupeQueries(queries);
    }

    private List<String> dedupeQueries(List<String> queries) {
        Set<String> seen = new LinkedHashSet<>();
        List<String> deduped = new ArrayList<>();
        for (String query : queries) {
            if (query == null || query.isBlank()) {
                continue;
            }
            String normalized = query.trim().replaceAll("\\s+", " ");
            if (seen.add(normalized.toLowerCase(Locale.ROOT))) {
                deduped.add(normalized);
            }
        }
        return deduped;
    }

    private void mergeUnique(List<InternetSearchResult> target, List<InternetSearchResult> candidates) {
        Set<String> urls = new LinkedHashSet<>();
        for (InternetSearchResult result : target) {
            urls.add(normalizeComparableUrl(result.url()));
        }
        for (InternetSearchResult candidate : candidates) {
            String normalizedUrl = normalizeComparableUrl(candidate.url());
            if (urls.add(normalizedUrl)) {
                target.add(candidate);
            }
        }
    }

    private List<InternetSearchResult> parseResults(String html, int maxResults) {
        List<InternetSearchResult> results = parseBingResults(html, maxResults);
        if (!results.isEmpty()) {
            return results;
        }
        return parseDuckDuckGoResults(html, maxResults);
    }

    private List<InternetSearchResult> rankResults(String query, List<InternetSearchResult> results) {
        if (results.isEmpty() || !wantsOfficialSources(query)) {
            return results;
        }
        return results.stream()
                .sorted(Comparator.comparingInt((InternetSearchResult result) -> officialSourceScore(query, result)).reversed())
                .toList();
    }

    private boolean wantsOfficialSources(String query) {
        String normalized = query == null ? "" : query.toLowerCase();
        return normalized.contains("官方")
                || normalized.contains("文档")
                || normalized.contains("official")
                || normalized.contains("documentation")
                || normalized.contains("reference");
    }

    private int officialSourceScore(String query, InternetSearchResult result) {
        String url = result.url() == null ? "" : result.url().toLowerCase();
        String host = hostOf(url);
        String title = result.title() == null ? "" : result.title().toLowerCase();
        String snippet = result.snippet() == null ? "" : result.snippet().toLowerCase();
        String topic = cleanOfficialSearchTopic(query).toLowerCase(Locale.ROOT);
        List<String> terms = queryTerms(query);
        int score = 0;
        String officialDomain = officialDomainFor(topic);
        if (officialDomain != null && (host.equals(officialDomain) || host.endsWith("." + officialDomain))) {
            score += 60;
        }
        if (host.startsWith("docs.") || host.contains(".docs.") || url.contains("/docs/") || url.contains("/reference/")) {
            score += 40;
        }
        if (!topic.isBlank() && (title.contains(topic) || url.contains(topic.replace(" ", "-")))) {
            score += 40;
        }
        for (String term : terms) {
            if (title.contains(term)) {
                score += 12;
            }
            if (url.contains(term)) {
                score += 8;
            }
            if (snippet.contains(term)) {
                score += 4;
            }
        }
        if (title.contains("official") || title.contains("documentation") || title.contains("reference")
                || snippet.contains("official") || snippet.contains("documentation") || snippet.contains("reference")) {
            score += 30;
        }
        if (host.endsWith("spring.io") || host.endsWith("apache.org") || host.endsWith("oracle.com")
                || host.endsWith("microsoft.com") || host.endsWith("github.io")) {
            score += 20;
        }
        if (host.contains("blog.") || host.contains("csdn.") || host.contains("juejin.")
                || host.contains("zhihu.") || host.contains("segmentfault.") || host.contains("liaoxuefeng.")
                || host.endsWith("springdoc.cn") || host.endsWith("springframework.org.cn")
                || host.endsWith("runoob.com") || host.endsWith("baidu.com")) {
            score -= 30;
        }
        if (isGenericHomePage(url) && matchedTerms(title + " " + url + " " + snippet, terms) <= 1) {
            score -= 100;
        }
        return score;
    }

    private String cleanOfficialSearchTopic(String query) {
        if (query == null || query.isBlank()) {
            return "";
        }
        return query
                .replace("帮我", "")
                .replace("请", "")
                .replace("联网搜索", "")
                .replace("联网查", "")
                .replace("上网搜索", "")
                .replace("网上搜索", "")
                .replace("网上查", "")
                .replace("搜索一下", "")
                .replace("查一下", "")
                .replace("查找", "")
                .replace("查询", "")
                .replace("最新资料", "")
                .replace("官方资料", "")
                .replace("官方文档", "")
                .replace("官方", "")
                .replace("文档", "")
                .replace("资料", "")
                .replace("关于", "")
                .replaceAll("(?i)\\b(official|documentation|reference|docs?|latest|search)\\b", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String officialDomainFor(String query) {
        String normalized = query == null ? "" : query.toLowerCase(Locale.ROOT);
        if (normalized.contains("spring")) {
            return "spring.io";
        }
        if (normalized.contains("mybatis")) {
            return "mybatis.org";
        }
        if (normalized.contains("redis")) {
            return "redis.io";
        }
        if (normalized.contains("docker")) {
            return "docker.com";
        }
        if (normalized.contains("kubernetes") || normalized.contains("k8s")) {
            return "kubernetes.io";
        }
        if (normalized.contains("dubbo")) {
            return "apache.org";
        }
        if (normalized.contains("kafka")) {
            return "apache.org";
        }
        if (normalized.contains("rabbitmq")) {
            return "rabbitmq.com";
        }
        if (normalized.contains("java")) {
            return "oracle.com";
        }
        return null;
    }

    private List<String> queryTerms(String query) {
        String topic = cleanOfficialSearchTopic(query).toLowerCase(Locale.ROOT);
        if (topic.isBlank()) {
            return List.of();
        }
        Set<String> terms = new LinkedHashSet<>();
        for (String part : topic.split("[^\\p{L}\\p{N}]+")) {
            if (part.length() >= 3 && !QUERY_STOP_WORDS.contains(part)) {
                terms.add(part);
            }
        }
        return List.copyOf(terms);
    }

    private int matchedTerms(String text, List<String> terms) {
        int count = 0;
        for (String term : terms) {
            if (text.contains(term)) {
                count++;
            }
        }
        return count;
    }

    private boolean isGenericHomePage(String url) {
        try {
            String path = new URI(url).getPath();
            return path == null || path.isBlank() || "/".equals(path);
        } catch (URISyntaxException ex) {
            return false;
        }
    }

    private List<InternetSearchResult> parseBingResults(String html, int maxResults) {
        if (html == null || html.isBlank()) {
            return List.of();
        }
        List<InternetSearchResult> results = new ArrayList<>();
        Matcher resultMatcher = BING_RESULT_PATTERN.matcher(html);
        while (resultMatcher.find() && results.size() < maxResults) {
            String block = resultMatcher.group(1);
            Matcher titleMatcher = BING_TITLE_LINK_PATTERN.matcher(block);
            if (!titleMatcher.find()) {
                continue;
            }
            String href = extractHref(titleMatcher.group(1));
            String title = InternetHtml.text(titleMatcher.group(2));
            String normalizedUrl = normalizeResultUrl(href);
            if (normalizedUrl == null || title.isBlank() || !isUsableResultUrl(normalizedUrl)) {
                continue;
            }
            String snippet = extractFirstParagraph(block);
            results.add(new InternetSearchResult(title, normalizedUrl, snippet));
        }
        return results;
    }

    private List<InternetSearchResult> parseDuckDuckGoResults(String html, int maxResults) {
        if (html == null || html.isBlank()) {
            return List.of();
        }
        List<InternetSearchResult> results = new ArrayList<>();
        Matcher matcher = ANCHOR_PATTERN.matcher(html);
        while (matcher.find() && results.size() < maxResults) {
            String attributes = matcher.group(1);
            if (attributes == null || !attributes.contains("result__a")) {
                continue;
            }
            String href = extractHref(attributes);
            String title = InternetHtml.text(matcher.group(2));
            String normalizedUrl = normalizeResultUrl(href);
            if (normalizedUrl == null || title.isBlank() || !isUsableResultUrl(normalizedUrl)) {
                continue;
            }
            String snippet = extractSnippetAfter(html, matcher.end());
            results.add(new InternetSearchResult(title, normalizedUrl, snippet));
        }
        return results;
    }

    private String extractHref(String attributes) {
        Matcher matcher = HREF_PATTERN.matcher(attributes);
        return matcher.find() ? HtmlUtils.htmlUnescape(matcher.group(1)) : null;
    }

    private String extractSnippetAfter(String html, int startIndex) {
        int endIndex = Math.min(html.length(), startIndex + 2_000);
        Matcher matcher = SNIPPET_PATTERN.matcher(html.substring(startIndex, endIndex));
        if (!matcher.find()) {
            return "";
        }
        return InternetHtml.text(matcher.group(1));
    }

    private String extractFirstParagraph(String html) {
        Matcher matcher = PARAGRAPH_PATTERN.matcher(html);
        return matcher.find() ? InternetHtml.text(matcher.group(1)) : "";
    }

    private String normalizeResultUrl(String href) {
        if (href == null || href.isBlank()) {
            return null;
        }
        if (href.startsWith("/l/?")) {
            String marker = "uddg=";
            int start = href.indexOf(marker);
            if (start >= 0) {
                int valueStart = start + marker.length();
                int valueEnd = href.indexOf('&', valueStart);
                String encoded = valueEnd >= 0 ? href.substring(valueStart, valueEnd) : href.substring(valueStart);
                return URLDecoder.decode(encoded, StandardCharsets.UTF_8);
            }
        }
        if (href.startsWith("//")) {
            return "https:" + href;
        }
        return href;
    }

    private String normalizeComparableUrl(String url) {
        if (url == null) {
            return "";
        }
        String normalized = url.trim().toLowerCase(Locale.ROOT);
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private boolean isUsableResultUrl(String url) {
        return url.startsWith("http://") || url.startsWith("https://");
    }

    private String hostOf(String url) {
        try {
            String host = new URI(url).getHost();
            return host == null ? "" : host.toLowerCase();
        } catch (URISyntaxException ex) {
            return "";
        }
    }
}
