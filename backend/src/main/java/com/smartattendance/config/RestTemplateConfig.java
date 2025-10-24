package com.smartattendance.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration class for REST clients.
 * Provides RestTemplate as a bean for dependency injection.
 * 
 * This follows the Dependency Inversion Principle - services depend on
 * the abstraction (injected RestTemplate) rather than concrete implementations.
 */
@Configuration
public class RestTemplateConfig {

    /**
     * Creates and configures a RestTemplate bean.
     * This bean can be injected into services that need to make HTTP requests.
     * 
     * @return configured RestTemplate instance
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

