package br.pucrs.constrsw.oauth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point da aplicacao. O escaneamento default cobre todo o package
 * {@code br.pucrs.constrsw.oauth.*}, que agora esta organizado em Clean
 * Architecture (domain / application / infrastructure).
 */
@SpringBootApplication
public class OauthApplication {

    public static void main(String[] args) {
        SpringApplication.run(OauthApplication.class, args);
    }
}
