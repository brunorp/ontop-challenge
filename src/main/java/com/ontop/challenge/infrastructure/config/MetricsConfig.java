package com.ontop.challenge.infrastructure.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.elastic.ElasticConfig;
import io.micrometer.elastic.ElasticMeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

/**
 * Configuration for Micrometer metrics export to Elasticsearch
 */
@Configuration
public class MetricsConfig {

    @Value("${management.metrics.export.elastic.host:http://localhost:9200}")
    private String elasticsearchHost;

    @Value("${management.metrics.export.elastic.index:metrics-ontop}")
    private String indexName;

    @Value("${management.metrics.export.elastic.step:10s}")
    private Duration step;

    @Bean
    public ElasticMeterRegistry elasticMeterRegistry() {
        ElasticConfig elasticConfig = new ElasticConfig() {
            @Override
            public String get(String key) {
                return null; // Accept defaults
            }

            @Override
            public String host() {
                return elasticsearchHost;
            }

            @Override
            public String index() {
                return indexName;
            }

            @Override
            public Duration step() {
                return step;
            }

            @Override
            public boolean autoCreateIndex() {
                return true;
            }
        };

        ElasticMeterRegistry registry = new ElasticMeterRegistry(elasticConfig, 
            io.micrometer.core.instrument.Clock.SYSTEM);
        
        // Add common tags to all metrics
        registry.config().commonTags(
            List.of(
                Tag.of("application", "ontop-challenge"),
                Tag.of("environment", "dev")
            )
        );

        return registry;
    }
}

