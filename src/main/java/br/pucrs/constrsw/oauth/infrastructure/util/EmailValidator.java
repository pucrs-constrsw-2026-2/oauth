package br.pucrs.constrsw.oauth.infrastructure.util;

import java.util.regex.Pattern;

/**
 * Valida e-mail usando uma versao pragmatica da regex RFC 5322 (a versao
 * "official standard" citada no enunciado). Compilada uma vez para evitar
 * recompilacao a cada chamada.
 */
public final class EmailValidator {

    private static final Pattern RFC_5322 = Pattern.compile(
            "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$");

    private EmailValidator() {}

    public static boolean isValid(String email) {
        return email != null && RFC_5322.matcher(email).matches();
    }
}
