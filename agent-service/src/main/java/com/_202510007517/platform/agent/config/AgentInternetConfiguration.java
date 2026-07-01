package com._202510007517.platform.agent.config;

import com._202510007517.platform.agent.internet.InternetAccessPolicy;
import com._202510007517.platform.agent.internet.InternetHttpClient;
import com._202510007517.platform.agent.internet.InternetSearchService;
import com._202510007517.platform.agent.internet.RestClientInternetHttpClient;
import com._202510007517.platform.agent.internet.WebPageReadService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AgentInternetProperties.class)
public class AgentInternetConfiguration {

    @Bean
    InternetAccessPolicy internetAccessPolicy(AgentInternetProperties properties) {
        return new InternetAccessPolicy(properties);
    }

    @Bean
    InternetHttpClient internetHttpClient(InternetAccessPolicy accessPolicy) {
        return new RestClientInternetHttpClient(accessPolicy);
    }

    @Bean
    InternetSearchService internetSearchService(AgentInternetProperties properties,
                                                InternetAccessPolicy accessPolicy,
                                                InternetHttpClient httpClient) {
        return new InternetSearchService(properties, accessPolicy, httpClient);
    }

    @Bean
    WebPageReadService webPageReadService(AgentInternetProperties properties,
                                          InternetAccessPolicy accessPolicy,
                                          InternetHttpClient httpClient) {
        return new WebPageReadService(properties, accessPolicy, httpClient);
    }
}
