package com.constrsw.oauth.config;

import java.time.Duration;

import org.apache.hc.client5.http.auth.StandardAuthScheme;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Configuration
public class RestTemplateConfig {

    /**
     * RestTemplate usado para todas as chamadas ao Keycloak. Ha dois pontos
     * criticos aqui:
     *
     *  1) Usamos Apache HttpClient 5 (nao o HttpURLConnection default do JDK).
     *     A implementacao "Simple" do Spring reenvia automaticamente a request
     *     ao receber WWW-Authenticate no 401 do Keycloak, o que provoca o
     *     erro "cannot retry due to server authentication, in streaming mode".
     *
     *  2) Desabilitamos os schemes de autenticacao do HttpClient e desligamos
     *     a autenticacao "expect". Nao queremos retry: queremos inspecionar
     *     status + body para mapear em nossas proprias excecoes.
     *
     *  3) Instalamos um ResponseErrorHandler no-op para o RestTemplate nao
     *     lancar excecao em 4xx/5xx.
     */
    @Bean
    public RestTemplate keycloakRestTemplate(RestTemplateBuilder builder) {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(5, TimeUnit.SECONDS)
                .setResponseTimeout(15, TimeUnit.SECONDS)
                .setTargetPreferredAuthSchemes(Collections.singletonList(StandardAuthScheme.BASIC))
                .setExpectContinueEnabled(false)
                .build();

        HttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                // A grande diferenca: desativa completamente o retry de
                // autenticacao. Se o Keycloak responder 401, o HttpClient devolve
                // a resposta como esta para o RestTemplate, sem tentar reenviar.
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

    /** Nunca considera a resposta como erro: o service inspeciona o status. */
    private static class NoOpResponseErrorHandler extends DefaultResponseErrorHandler {
        @Override
        public boolean hasError(org.springframework.http.client.ClientHttpResponse response) {
            return false;
        }
    }
}
