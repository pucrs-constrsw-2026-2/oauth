package com.constrsw.oauth;

import org.springframework.boot.SpringApplication;

// Classe do stash preservada. @SpringBootApplication removido apos merge para
// permitir que o spring-boot-maven-plugin identifique um unico main class
// (br.pucrs.constrsw.oauth.OauthApplication e a app ativa). O main() abaixo
// nao e chamado - continua compilando para preservar o codigo original.
public class OAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(OAuthApplication.class, args);
    }
}
