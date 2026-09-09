package com.constrsw.oauth.util;

import java.util.regex.Pattern;

/**
 * Valida e-mail usando a regex "official" RFC 5322 (versao pragmatica adotada
 * pelo enunciado do trabalho). Compilada uma vez para evitar recompilacao a
 * cada chamada.
 */
public final class EmailValidator {

    // RFC 5322 official standard regex - referida diretamente no enunciado.
    private static final Pattern RFC_5322 = Pattern.compile(
            "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$");

    private EmailValidator() {}

    public static boolean isValid(String email) {
        return email != null && RFC_5322.matcher(email).matches();
    }
}
