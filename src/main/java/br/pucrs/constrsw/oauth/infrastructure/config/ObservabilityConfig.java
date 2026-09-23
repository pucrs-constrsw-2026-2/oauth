package br.pucrs.constrsw.oauth.infrastructure.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.springframework.boot.actuate.metrics.export.prometheus.PrometheusScrapeEndpoint;

@Configuration
public class ObservabilityConfig {

  @Bean
  @ConditionalOnMissingBean(PrometheusMeterRegistry.class)
  public PrometheusMeterRegistry prometheusMeterRegistry() {
    return new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
  }

  @Bean
  @ConditionalOnMissingBean(PrometheusScrapeEndpoint.class)
  public PrometheusScrapeEndpoint prometheusScrapeEndpoint(PrometheusMeterRegistry registry) {
    return new PrometheusScrapeEndpoint(registry.getPrometheusRegistry());
  }
}
