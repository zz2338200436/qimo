package com._202510007517.platform.agent.internet;

import java.net.URI;
import java.time.Duration;

public interface InternetHttpClient {
    String get(URI uri, Duration timeout, int maxBytes, String userAgent);
}
