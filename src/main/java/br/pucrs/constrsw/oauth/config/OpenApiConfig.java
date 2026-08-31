package br.pucrs.constrsw.oauth.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI oauthOpenApi() {
        final String bearerScheme = "bearer-key";
        return new OpenAPI()
                .info(new Info()
                        .title("ConstrSW - OAuth API")
                        .description("API REST que encapsula o Keycloak para autenticacao, usuarios e roles (Grupo 04 - students).")
                        .version("v1"))
                .components(new Components()
                        .addSecuritySchemes(bearerScheme, new SecurityScheme()
                                .name(bearerScheme)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
