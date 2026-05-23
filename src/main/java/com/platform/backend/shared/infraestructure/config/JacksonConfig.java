package com.platform.backend.shared.infraestructure.config;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Value("${app.json.naming-strategy:camelCase}")
    private String namingStrategy;

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
        return builder -> {
            builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            if ("snake_case".equalsIgnoreCase(namingStrategy)) {
                builder.propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
            }
            // camelCase is Jackson's default — no explicit override needed
        };
    }
}
