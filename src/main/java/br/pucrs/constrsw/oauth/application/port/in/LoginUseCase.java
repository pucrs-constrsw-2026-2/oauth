package br.pucrs.constrsw.oauth.application.port.in;

import br.pucrs.constrsw.oauth.domain.model.AuthTokens;
import br.pucrs.constrsw.oauth.domain.model.Credentials;

public interface LoginUseCase {
    AuthTokens execute(Credentials credentials);
}
