package br.pucrs.constrsw.oauth.infrastructure.config;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.apache.hc.client5.http.auth.StandardAuthScheme;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

/**
 * Beans de infraestrutura HTTP:
 *
 *  - RestTemplate ("restTemplate"): usado pelo {@code KeycloakAuthGateway} para
 *    o token endpoint (multipart / form). Apache HttpClient 5 para nao sofrer
 *    com o retry automatico do JDK HttpURLConnection quando o Keycloak devolve
 *    401 em streaming.
 *
 *  - ResponseErrorHandler no-op: nao lancamos excecao em 4xx/5xx. O gateway
 *    inspeciona status + body para traduzir em excecoes de dominio.
 */
@Configuration
@EnableConfigurationProperties(KeycloakProperties.class)
public class HttpClientConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(5, TimeUnit.SECONDS)
                .setResponseTimeout(15, TimeUnit.SECONDS)
                .setTargetPreferredAuthSchemes(Collections.singletonList(StandardAuthScheme.BASIC))
                .setExpectContinueEnabled(false)
                .build();

        HttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .disableAuthCaching()
                .disableRedirectHandling()
                .build();

        HttpComponentsClientHttpRequestFactory factory =
                new HttpComponentsClientHttpRequestFactory(httpClient);
        factory.setConnectionRequestTimeout((int) Duration.ofSeconds(15).toMillis());

        return builder
                .requestFactory(() -> factory)
                .errorHandler(new NoOpResponseErrorHandler())
                .build();
    }

    private static class NoOpResponseErrorHandler extends DefaultResponseErrorHandler {
        @Override
        public boolean hasError(org.springframework.http.client.ClientHttpResponse response) {
            return false;
        }
    }
}
