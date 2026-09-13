package br.pucrs.constrsw.oauth.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI oauthOpenApi() {
        return new OpenAPI().info(new Info()
                .title("OAuth API")
                .description("API REST de integração com o Keycloak")
                .version("1.0.0"));
    }
}
