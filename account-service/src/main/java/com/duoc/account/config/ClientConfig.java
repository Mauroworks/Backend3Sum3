package com.duoc.account.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ClientConfig {

    /**
     * RestClient compartido por las llamadas service-to-service
     * (ServiceTokenProvider y CsvResultApiClient).
     */
    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder().defaultHeader("X-Origen", "account-service");
    }
}