package com.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ExchangeRateClientConfig {

    @Bean
    public RestClient dolarApiClient() {
        return RestClient.create("https://dolarapi.com");
    }

    @Bean
    public RestClient frankfurterClient() {
        return RestClient.create("https://api.frankfurter.dev");
    }
}