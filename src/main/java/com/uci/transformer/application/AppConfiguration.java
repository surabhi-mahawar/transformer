package com.uci.transformer.application;

import com.uci.utils.CampaignService;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableAutoConfiguration
public class AppConfiguration {

    @Value("${campaign.url}")
    public String CAMPAIGN_URL;

    @Bean
    @Qualifier("rest")
    public RestTemplate getRestTemplate() {
        return new RestTemplate();
    }

    @Bean
    public CampaignService getCampaignService() {

        WebClient webClient = WebClient.builder()
                .baseUrl(CAMPAIGN_URL)
                .build();
        return new CampaignService(webClient);
    }

    @Bean
    @Qualifier("custom")
    public RestTemplate getCustomTemplate() {
        RestTemplateBuilder builder = new RestTemplateBuilder();
        Credentials credentials = new UsernamePasswordCredentials("test","abcd1234");
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, credentials);

        HttpClient httpClient = HttpClients
                .custom()
                .disableCookieManagement()
                .setDefaultCredentialsProvider(credentialsProvider)
                .build();

        return builder
                .requestFactory(() -> new HttpComponentsClientHttpRequestFactory(httpClient))
                .build();
    }
}
